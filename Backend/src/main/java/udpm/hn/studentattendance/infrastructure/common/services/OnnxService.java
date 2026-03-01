package udpm.hn.studentattendance.infrastructure.common.services;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.Batchifier;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Service
public class OnnxService {

    private static final int SIZE_ANTISPOOF = 224;
    private static final int SIZE_ANTISPOOF2 = 256;
    private static final int SIZE_ANTISPOOF3 = 128;
    private static final int SIZE_ANTISPOOF4 = 224;
    private static final int SIZE_ARCFACE = 112;
    private static final int SIZE_DETECT = 128;

    @Value("${app.config.path.model}")
    private String modelPath;

    private ZooModel<byte[], float[]> antiSpoofModel;
    private ZooModel<byte[], float[]> antiSpoof2Model;
    private ZooModel<byte[], float[]> antiSpoof3Model;
    private ZooModel<byte[], float[]> antiSpoof4Model;
    private ZooModel<byte[], float[]> arcFaceModel;
    private ZooModel<byte[], float[]> detectModel;

    private BlockingQueue<Predictor<byte[], float[]>> antiSpoofPredictorPool;
    private BlockingQueue<Predictor<byte[], float[]>> antiSpoof2PredictorPool;
    private BlockingQueue<Predictor<byte[], float[]>> antiSpoof3PredictorPool;
    private BlockingQueue<Predictor<byte[], float[]>> antiSpoof4PredictorPool;
    private BlockingQueue<Predictor<byte[], float[]>> arcFacePredictorPool;
    private BlockingQueue<Predictor<byte[], float[]>> detectPredictorPool;

// Optimize POOL_SIZE for Render Free Tier (512MB)
    private final int POOL_SIZE = 1;

    public synchronized void ensureAntiSpoofModel() throws IOException, ModelNotFoundException, MalformedModelException {
        if (antiSpoofModel == null) {
            antiSpoofModel = ModelZoo.loadModel(buildAntiSpoofCriteria());
            antiSpoofPredictorPool = new ArrayBlockingQueue<>(POOL_SIZE);
            for (int i = 0; i < POOL_SIZE; i++) antiSpoofPredictorPool.add(antiSpoofModel.newPredictor());
        }
    }

    public synchronized void ensureAntiSpoof2Model() throws IOException, ModelNotFoundException, MalformedModelException {
        if (antiSpoof2Model == null) {
            antiSpoof2Model = ModelZoo.loadModel(buildAntiSpoof2Criteria());
            antiSpoof2PredictorPool = new ArrayBlockingQueue<>(POOL_SIZE);
            for (int i = 0; i < POOL_SIZE; i++) antiSpoof2PredictorPool.add(antiSpoof2Model.newPredictor());
        }
    }

    public synchronized void ensureAntiSpoof3Model() throws IOException, ModelNotFoundException, MalformedModelException {
        if (antiSpoof3Model == null) {
            antiSpoof3Model = ModelZoo.loadModel(buildAntiSpoof3Criteria());
            antiSpoof3PredictorPool = new ArrayBlockingQueue<>(POOL_SIZE);
            for (int i = 0; i < POOL_SIZE; i++) antiSpoof3PredictorPool.add(antiSpoof3Model.newPredictor());
        }
    }

    public synchronized void ensureAntiSpoof4Model() throws IOException, ModelNotFoundException, MalformedModelException {
        if (antiSpoof4Model == null) {
            antiSpoof4Model = ModelZoo.loadModel(buildAntiSpoof4Criteria());
            antiSpoof4PredictorPool = new ArrayBlockingQueue<>(POOL_SIZE);
            for (int i = 0; i < POOL_SIZE; i++) antiSpoof4PredictorPool.add(antiSpoof4Model.newPredictor());
        }
    }

    public synchronized void ensureArcFaceModel() throws IOException, ModelNotFoundException, MalformedModelException {
        if (arcFaceModel == null) {
            arcFaceModel = ModelZoo.loadModel(buildArcFaceCriteria());
            arcFacePredictorPool = new ArrayBlockingQueue<>(POOL_SIZE);
            for (int i = 0; i < POOL_SIZE; i++) arcFacePredictorPool.add(arcFaceModel.newPredictor());
        }
    }

    public synchronized void ensureDetectModel() throws IOException, ModelNotFoundException, MalformedModelException {
        if (detectModel == null) {
            detectModel = ModelZoo.loadModel(buildDetectCriteria());
            detectPredictorPool = new ArrayBlockingQueue<>(POOL_SIZE);
            for (int i = 0; i < POOL_SIZE; i++) detectPredictorPool.add(detectModel.newPredictor());
        }
    }

    @PostConstruct
    public void init() {
        // Models will be loaded on demand (Lazy Loading) to save memory at startup
    }

    @PreDestroy
    public void close() {
        if (antiSpoofPredictorPool != null) antiSpoofPredictorPool.forEach(Predictor::close);
        if (antiSpoof2PredictorPool != null) antiSpoof2PredictorPool.forEach(Predictor::close);
        if (antiSpoof3PredictorPool != null) antiSpoof3PredictorPool.forEach(Predictor::close);
        if (antiSpoof4PredictorPool != null) antiSpoof4PredictorPool.forEach(Predictor::close);
        if (arcFacePredictorPool != null) arcFacePredictorPool.forEach(Predictor::close);
        if (detectPredictorPool != null) detectPredictorPool.forEach(Predictor::close);
        if (antiSpoofModel != null) antiSpoofModel.close();
        if (antiSpoof2Model != null) antiSpoof2Model.close();
        if (antiSpoof3Model != null) antiSpoof3Model.close();
        if (antiSpoof4Model != null) antiSpoof4Model.close();
        if (arcFaceModel != null) arcFaceModel.close();
        if (detectModel != null) detectModel.close();
    }

    private Criteria<byte[], float[]> buildAntiSpoofCriteria() {
        Translator<byte[], float[]> translator = new Translator<>() {
            @Override
            public NDList processInput(TranslatorContext ctx, byte[] input) throws IOException {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(input));
                BufferedImage resized = resizeImage(img, SIZE_ANTISPOOF);
                float[] data = bufferedImageToCHWFloatArray(resized);
                return new NDList(ctx.getNDManager().create(data, new Shape(1, 3, SIZE_ANTISPOOF, SIZE_ANTISPOOF)));
            }
            @Override
            public float[] processOutput(TranslatorContext ctx, NDList list) {
                return list.singletonOrThrow().toFloatArray();
            }
            @Override
            public Batchifier getBatchifier() { return null; }
        };

        return Criteria.builder()
                .setTypes(byte[].class, float[].class)
                .optModelPath(Paths.get(modelPath, "antiSpoof.onnx").toAbsolutePath())
                .optEngine("OnnxRuntime")
                .optOption("executionProvider", "CPUExecutionProvider")
                .optOption("device", "cpu")
                .optTranslator(translator)
                .build();
    }

    private Criteria<byte[], float[]> buildAntiSpoof2Criteria() {
        Translator<byte[], float[]> translator = new Translator<>() {
            @Override
            public NDList processInput(TranslatorContext ctx, byte[] input) throws IOException {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(input));
                BufferedImage resized = resizeImage(img, SIZE_ANTISPOOF2);
                float[] data = bufferedImageToCHWFloatArray(resized);
                return new NDList(ctx.getNDManager().create(data, new Shape(1, 3, SIZE_ANTISPOOF2, SIZE_ANTISPOOF2)));
            }
            @Override
            public float[] processOutput(TranslatorContext ctx, NDList list) {
                return list.singletonOrThrow().toFloatArray();
            }
            @Override
            public Batchifier getBatchifier() { return null; }
        };

        return Criteria.builder()
                .setTypes(byte[].class, float[].class)
                .optModelPath(Paths.get(modelPath, "m8.onnx").toAbsolutePath())
                .optEngine("OnnxRuntime")
                .optOption("executionProvider", "CPUExecutionProvider")
                .optOption("device", "cpu")
                .optTranslator(translator)
                .build();
    }

    private Criteria<byte[], float[]> buildAntiSpoof3Criteria() {
        Translator<byte[], float[]> translator = new Translator<>() {
            @Override
            public NDList processInput(TranslatorContext ctx, byte[] input) throws IOException {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(input));
                BufferedImage resized = resizeImage(img, SIZE_ANTISPOOF3);
                float[] data = bufferedImageToCHWFloatArray(resized);
                return new NDList(ctx.getNDManager().create(data, new Shape(1, 3, SIZE_ANTISPOOF3, SIZE_ANTISPOOF3)));
            }
            @Override
            public float[] processOutput(TranslatorContext ctx, NDList list) {
                return list.singletonOrThrow().toFloatArray();
            }
            @Override
            public Batchifier getBatchifier() { return null; }
        };

        return Criteria.builder()
                .setTypes(byte[].class, float[].class)
                .optModelPath(Paths.get(modelPath, "AntiSpoofing_bin_1.5_128.onnx").toAbsolutePath())
                .optEngine("OnnxRuntime")
                .optOption("executionProvider", "CPUExecutionProvider")
                .optOption("device", "cpu")
                .optTranslator(translator)
                .build();
    }

    private Criteria<byte[], float[]> buildAntiSpoof4Criteria() {
        Translator<byte[], float[]> translator = new Translator<>() {
            @Override
            public NDList processInput(TranslatorContext ctx, byte[] input) throws IOException {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(input));
                BufferedImage resized = resizeImage(img, SIZE_ANTISPOOF4);
                float[] data = bufferedImageToCHWFloatArray(resized);
                return new NDList(ctx.getNDManager().create(data, new Shape(1, 3, SIZE_ANTISPOOF4, SIZE_ANTISPOOF4)));
            }
            @Override
            public float[] processOutput(TranslatorContext ctx, NDList list) {
                return list.get(1).toFloatArray();
            }
            @Override
            public Batchifier getBatchifier() { return null; }
        };

        return Criteria.builder()
                .setTypes(byte[].class, float[].class)
                .optModelPath(Paths.get(modelPath, "OULU_Protocol_2_model_0_0.onnx").toAbsolutePath())
                .optEngine("OnnxRuntime")
                .optOption("executionProvider", "CPUExecutionProvider")
                .optOption("device", "cpu")
                .optTranslator(translator)
                .build();
    }

    private Criteria<byte[], float[]> buildDetectCriteria() {
        Translator<byte[], float[]> translator = new Translator<>() {

            @Override
            public NDList processInput(TranslatorContext ctx, byte[] input) throws IOException {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(input));
                BufferedImage resized = resizeImage(img, SIZE_DETECT);
                float[] data = bufferedImageToCHWFloatArray(resized);

                NDManager manager = ctx.getNDManager();
                NDArray imageND = manager.create(data, new Shape(1, 3, SIZE_DETECT, SIZE_DETECT));

                NDArray confThreshold = manager.create(new float[]{0.0f});
                NDArray maxDetections = manager.create(new long[]{1});
                NDArray iouThreshold = manager.create(new float[]{0.3f});

                return new NDList(imageND, confThreshold, maxDetections, iouThreshold);
            }
            @Override
            public float[] processOutput(TranslatorContext ctx, NDList list) {
                float[] out = list.get(0).toFloatArray();
                float bestConf = 0f;
                int bestIdx = -1;
                int numBoxes = out.length / 16;
                for (int i = 0; i < numBoxes; i++) {
                    float conf = out[i * 16 + 4];
                    if (conf > bestConf) {
                        bestConf = conf;
                        bestIdx = i;
                    }
                }
                if (bestIdx == -1) return null;

                float x1 = out[bestIdx * 16];
                float y1 = out[bestIdx * 16 + 1];
                float x2 = out[bestIdx * 16 + 2];
                float y2 = out[bestIdx * 16 + 3];

                return new float[]{x1, y1, x2, y2};
            }
            @Override
            public Batchifier getBatchifier() { return null; }
        };

        return Criteria.builder()
                .setTypes(byte[].class, float[].class)
                .optModelPath(Paths.get(modelPath, "blaze.onnx").toAbsolutePath())
                .optEngine("OnnxRuntime")
                .optOption("executionProvider", "CPUExecutionProvider")
                .optOption("device", "cpu")
                .optTranslator(translator)
                .build();
    }

    private Criteria<byte[], float[]> buildArcFaceCriteria() {
        Translator<byte[], float[]> translator = new Translator<>() {
            @Override
            public NDList processInput(TranslatorContext ctx, byte[] input) throws IOException {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(input));
                BufferedImage resized = resizeImage(img, SIZE_ARCFACE);
                float[] data = bufferedImageToCHWFloatArray(resized, true);
                return new NDList(ctx.getNDManager().create(data, new Shape(1, 3, SIZE_ARCFACE, SIZE_ARCFACE)));
            }
            @Override
            public float[] processOutput(TranslatorContext ctx, NDList list) {
                return list.singletonOrThrow().toFloatArray();
            }
            @Override
            public Batchifier getBatchifier() { return null; }
        };

        return Criteria.builder()
                .setTypes(byte[].class, float[].class)
                .optModelPath(Paths.get(modelPath, "embedding.onnx").toAbsolutePath())
                .optEngine("OnnxRuntime")
                .optOption("executionProvider", "CPUExecutionProvider")
                .optOption("device", "cpu")
                .optTranslator(translator)
                .build();
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int size) {
        BufferedImage resized = new BufferedImage(size, size, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, size, size, null);
        g2d.dispose();
        return resized;
    }

    private float[] bufferedImageToCHWFloatArray(BufferedImage image) {
        int w = image.getWidth(), h = image.getHeight();
        float[] mean = {0.485f, 0.456f, 0.406f};
        float[] std = {0.229f, 0.224f, 0.225f};
        float[] data = new float[3 * w * h];
        for (int c = 0; c < 3; c++) {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int rgb = image.getRGB(x, y);
                    int value = (c == 0) ? (rgb >> 16) & 0xFF :
                            (c == 1) ? (rgb >> 8) & 0xFF :
                                    rgb & 0xFF;
                    data[c * w * h + y * w + x] = (value / 255f - mean[c]) / std[c];
                }
            }
        }
        return data;
    }

    private float[] bufferedImageToCHWFloatArray(BufferedImage image, boolean normalizeArcface) {
        int w = image.getWidth(), h = image.getHeight();
        float[] data = new float[3 * w * h];
        for (int c = 0; c < 3; c++) {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int rgb = image.getRGB(x, y);
                    int value = (c == 0) ? (rgb >> 16) & 0xFF :
                            (c == 1) ? (rgb >> 8) & 0xFF :
                                    rgb & 0xFF;
                    data[c * w * h + y * w + x] = normalizeArcface ?
                            (value - 127.5f) / 128f :
                            value / 255f;
                }
            }
        }
        return data;
    }

    public static float[] normalize(float[] vec) {
        double norm = 0;
        for (float v : vec) norm += v * v;
        norm = Math.sqrt(norm);
        float[] out = new float[vec.length];
        for (int i = 0; i < vec.length; i++) out[i] = (float) (vec[i] / norm);
        return out;
    }

    public static boolean isColorFake(byte[] imgBytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imgBytes)) {
            BufferedImage image = ImageIO.read(bais);
            image = cropWithPadding(image, 350);

            long sumR = 0, sumG = 0, sumB = 0;
            int count = 0;

            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    Color c = new Color(image.getRGB(x, y), true);
                    sumR += c.getRed();
                    sumG += c.getGreen();
                    sumB += c.getBlue();
                    count++;
                }
            }

            if (count == 0) return true;

            int avgR = (int) (sumR / count);
            int avgG = (int) (sumG / count);
            int avgB = (int) (sumB / count);

            float[] hsv = Color.RGBtoHSB(avgR, avgG, avgB, null);
            float hue = hsv[0] * 360;
            float sat = hsv[1];
            float brightness = hsv[2];

            if (sat > 0.1 && hue >= 120 && hue <= 240) return true;
            return sat <= 0.2 && brightness >= 0.85;
        }
    }

    public static BufferedImage cropWithPadding(BufferedImage src, int padding) {
        if (padding < 1) {
            return src;
        }
        int w = src.getWidth();
        int h = src.getHeight();
        int side = Math.min(w, h) - 2 * padding;
        if (side <= 0) {
            return src;
        }
        int x = (w - side) / 2;
        int y = (h - side) / 2;
        BufferedImage dest = new BufferedImage(side, side, src.getType());
        Graphics g = dest.getGraphics();
        g.drawImage(src, 0, 0, side, side, x, y, x + side, y + side, null);
        g.dispose();
        return dest;
    }

    private static float[] softmax(float[] logits) {
        float max = Float.NEGATIVE_INFINITY;
        for (float l : logits) if (l > max) max = l;

        float sum = 0f;
        float[] exps = new float[logits.length];
        for (int i = 0; i < logits.length; i++) {
            exps[i] = (float)Math.exp(logits[i] - max);
            sum += exps[i];
        }
        for (int i = 0; i < exps.length; i++) exps[i] /= sum;
        return exps;
    }

    public float antiSpoof(byte[] imgBytes) throws InterruptedException, TranslateException {
        try {
            ensureAntiSpoofModel();
        } catch (Exception e) {
            return 0.5f; // Fallback
        }
        Predictor<byte[], float[]> predictor = antiSpoofPredictorPool.take();
        try {
            float[] result = predictor.predict(imgBytes);
            return result[0];
        } finally {
            antiSpoofPredictorPool.put(predictor);
        }
    }

    public float antiSpoof2(byte[] imgBytes) throws InterruptedException, TranslateException {
        try {
            ensureAntiSpoof2Model();
        } catch (Exception e) {
            return 0.5f; // Fallback
        }
        Predictor<byte[], float[]> predictor = antiSpoof2PredictorPool.take();
        try {
            float[] result = predictor.predict(imgBytes);
            float[] softmax = softmax(result);
            return softmax[0];
        } finally {
            antiSpoof2PredictorPool.put(predictor);
        }
    }

    public float antiSpoof3(byte[] imgBytes) throws InterruptedException, TranslateException {
        try {
            ensureAntiSpoof3Model();
        } catch (Exception e) {
            return 0.5f; // Fallback
        }
        Predictor<byte[], float[]> predictor = antiSpoof3PredictorPool.take();
        try {
            float[] result = predictor.predict(imgBytes);
            float[] softmax = softmax(result);
            return softmax[0];
        } finally {
            antiSpoof3PredictorPool.put(predictor);
        }
    }

    public float antiSpoof4(byte[] imgBytes) throws InterruptedException, TranslateException {
        try {
            ensureAntiSpoof4Model();
        } catch (Exception e) {
            return 0.5f; // Fallback
        }
        Predictor<byte[], float[]> predictor = antiSpoof4PredictorPool.take();
        try {
            float[] result = predictor.predict(imgBytes);
            return result[0];
        } finally {
            antiSpoof4PredictorPool.put(predictor);
        }
    }

    public byte[] detected(byte[] imgBytes) throws Exception {
        ensureDetectModel();
        Predictor<byte[], float[]> predictor = detectPredictorPool.take();
        try {
            int shiftX = -40;
            int shiftY = 0;
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgBytes));
            int w = img.getWidth(), h = img.getHeight();

            float[] bbox = predictor.predict(imgBytes);
            if (bbox == null || bbox.length != 4) {
                return null;
            }

            int x1 = Math.round(bbox[0] * w);
            int y1 = Math.round(bbox[1] * h);
            int x2 = Math.round(bbox[2] * w);
            int y2 = Math.round(bbox[3] * h);

            int iw = x2 - x1;
            int ih = y2 - y1;

            if (iw <= 0 || ih <= 0) {
                return null;
            }

            x1 = Math.max(0, x1 + shiftX);
            y1 = Math.max(0, y1 + shiftY);
            x2 = Math.min(w, x1 + iw);
            y2 = Math.min(h, y1 + ih);

            BufferedImage cropped = img.getSubimage(x1, y1, x2 - x1, y2 - y1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(cropped, "png", baos);
            return baos.toByteArray();
        } finally {
            detectPredictorPool.put(predictor);
        }
    }

    public float[] getEmbedding(byte[] imgBytes) throws InterruptedException, TranslateException {
        ensureArcFaceModel();
        Predictor<byte[], float[]> predictor = arcFacePredictorPool.take();
        try {
            return normalize(predictor.predict(imgBytes));
        } finally {
            arcFacePredictorPool.put(predictor);
        }
    }

    public boolean isFake(byte[] faceBox, byte[] canvas)  {
        try {
            byte[] faceDetected = detected(canvas);
            if (faceDetected == null) {
                return true;
            }

            if (isColorFake(faceDetected)) {
                return true;
            }

            float antiSpoof = antiSpoof(faceBox);
            float antiSpoof2 = antiSpoof2(faceDetected);
            float antiSpoof3 = antiSpoof3(faceDetected);
            float antiSpoof4 = antiSpoof4(faceDetected);

            if(antiSpoof > 0.7  && antiSpoof2 > 0.7 && antiSpoof3 > 0.7 && antiSpoof4 > 0.7) {
                return false;
            }

            if (antiSpoof > 0.9999 || antiSpoof2 > 0.99  || antiSpoof3 > 0.99 || antiSpoof4 > 0.9999) {
                return false;
            }

            int score = 0;
            if (antiSpoof > 0.8) {
                score++;
            }
            if (antiSpoof2 > 0.8) {
                score++;
            }
            if (antiSpoof3 > 0.7) {
                score++;
            }
            if (antiSpoof4 > 0.8) {
                score++;
            }

            if (score > 2) {
                return antiSpoof < 0.00009 || antiSpoof2 < 0.2  || antiSpoof4 < 0.0009; // || antiSpoof3 < 0.000000009
            }

            int scoreF = 0;
            if (antiSpoof < 0.1) {
                scoreF++;
            }
            if (antiSpoof2 < 0.1) {
                scoreF++;
            }
            if (antiSpoof3 < 0.1) {
                scoreF++;
            }
            if (antiSpoof4 < 0.1) {
                scoreF++;
            }

            if (scoreF > 1) {
                return true;
            }

            if (antiSpoof < 0.00001 || antiSpoof2 < 0.01 || antiSpoof3 < 0.00001 || antiSpoof4 < 0.00001) {
                return true;
            }

            List<Boolean> checking = new ArrayList<>();
            checking.add(antiSpoof < 0.6);
            checking.add(antiSpoof2 < 0.6);
            checking.add(antiSpoof3 < 0.6);
            checking.add(antiSpoof4 < 0.6);
            
            long totalReject = checking.stream().filter(Boolean::booleanValue).count();

            return totalReject > 1 || (antiSpoof < 0.01 || antiSpoof2 < 0.01 || antiSpoof3 < 0.01 ||  antiSpoof4 < 0.016);
        } catch (Exception e) {
            return true;
        }
    }

}