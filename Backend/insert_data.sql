-- Dữ liệu mẫu cho hệ thống Quản lý Điểm danh Sinh viên (Student Attendance Management)

-- 1. Semester (Học kỳ)
INSERT INTO `semester` (`id`, `status`, `created_at`, `updated_at`, `code`, `name`, `from_date`, `to_date`, `year`) VALUES
('SEM001', 1, 1710300000000, 1710300000000, 'SPRING2024', 'SPRING', 1704067200000, 1714435200000, 2024),
('SEM002', 1, 1710300000000, 1710300000000, 'SUMMER2024', 'SUMMER', 1714521600000, 1725062400000, 2024),
('SEM003', 1, 1710300000000, 1710300000000, 'FALL2024', 'FALL', 1725148800000, 1735516800000, 2024);

-- 2. Facility (Cơ sở)
INSERT INTO `facility` (`id`, `status`, `created_at`, `updated_at`, `code`, `name`, `position`) VALUES
('FAC001', 1, 1710300000000, 1710300000000, 'HANOI', 'FPT Polytechnic Hà Nội', 1),
('FAC002', 1, 1710300000000, 1710300000000, 'DANANG', 'FPT Polytechnic Đà Nẵng', 2),
('FAC003', 1, 1710300000000, 1710300000000, 'HCM', 'FPT Polytechnic TP Hồ Chí Minh', 3);

-- 3. Level Project (Mức độ dự án)
INSERT INTO `level_project` (`id`, `status`, `created_at`, `updated_at`, `code`, `name`, `description`) VALUES
('LVL001', 1, 1710300000000, 1710300000000, 'BASIC', 'Cơ bản', 'Dự án dành cho sinh viên mới'),
('LVL002', 1, 1710300000000, 1710300000000, 'ADVANCED', 'Nâng cao', 'Dự án yêu cầu kỹ năng chuyên sâu'),
('LVL003', 1, 1710300000000, 1710300000000, 'WORKSHOP', 'Workshop', 'Dự án thực tế ngắn hạn');

-- 4. Subject (Môn học)
INSERT INTO `subject` (`id`, `status`, `created_at`, `updated_at`, `code`, `name`) VALUES
('SUB001', 1, 1710300000000, 1710300000000, 'PRO1014', 'Dự án mẫu'),
('SUB002', 1, 1710300000000, 1710300000000, 'PRO2011', 'Dự án 1'),
('SUB003', 1, 1710300000000, 1710300000000, 'PRO2021', 'Dự án 2');

-- 5. Subject Facility (Môn học tại cơ sở)
INSERT INTO `subject_facility` (`id`, `status`, `created_at`, `updated_at`, `id_facility`, `id_subject`) VALUES
('SF001', 1, 1710300000000, 1710300000000, 'FAC001', 'SUB001'),
('SF002', 1, 1710300000000, 1710300000000, 'FAC001', 'SUB002'),
('SF003', 1, 1710300000000, 1710300000000, 'FAC001', 'SUB003');

-- 6. User Staff (Nhân viên/Giảng viên)
INSERT INTO `user_staff` (`id`, `status`, `created_at`, `updated_at`, `email_fe`, `email_fpt`, `name`, `code`, `image`) VALUES
('STAFF001', 1, 1710300000000, 1710300000000, 'thanhnx8@fe.edu.vn', 'thanhnx8@fpt.edu.vn', 'Nguyễn Xuân Thành', 'ThanhNX8', 'https://placeholder.com/staff1'),
('STAFF002', 1, 1710300000000, 1710300000000, 'duonglt@fe.edu.vn', 'duonglt@fpt.edu.vn', 'Lê Tùng Dương', 'DuongLT', 'https://placeholder.com/staff2'),
('STAFF003', 1, 1710300000000, 1710300000000, 'hanhdt@fe.edu.vn', 'hanhdt@fpt.edu.vn', 'Đào Thị Hạnh', 'HanhDT', 'https://placeholder.com/staff3');

-- 7. Role (Vai trò của nhân viên tại cơ sở)
-- Ordinal: 0: ADMIN, 1: STAFF, 2: STUDENT, 3: TEACHER
INSERT INTO `role` (`id`, `status`, `created_at`, `updated_at`, `code`, `id_facility`, `id_user_staff`) VALUES
('ROLE001', 1, 1710300000000, 1710300000000, 0, 'FAC001', 'STAFF001'), -- ADMIN
('ROLE002', 1, 1710300000000, 1710300000000, 3, 'FAC001', 'STAFF002'), -- TEACHER
('ROLE003', 1, 1710300000000, 1710300000000, 1, 'FAC001', 'STAFF003'); -- STAFF

-- 8. User Admin (Admin hệ thống)
INSERT INTO `user_admin` (`id`, `status`, `created_at`, `updated_at`, `email`, `name`, `code`, `image`) VALUES
('ADM001', 1, 1710300000000, 1710300000000, 'admin@fpt.edu.vn', 'Super Admin', 'ADMIN01', NULL);

-- 9. Facility IP (IP cho phép tại cơ sở)
-- type: 0: IPV4, 1: IPV6
INSERT INTO `facility_ip` (`id`, `status`, `created_at`, `updated_at`, `type`, `ip`, `id_facility`) VALUES
('IP001', 1, 1710300000000, 1710300000000, 0, '192.168.1.1', 'FAC001'),
('IP002', 1, 1710300000000, 1710300000000, 0, '10.0.0.1', 'FAC001');

-- 10. Facility Location (Vị trí cơ sở)
INSERT INTO `facility_location` (`id`, `status`, `created_at`, `updated_at`, `name`, `latitude`, `longitude`, `radius`, `id_facility`) VALUES
('LOC001', 1, 1710300000000, 1710300000000, 'Tòa P', 21.0379, 105.7468, 100, 'FAC001'),
('LOC002', 1, 1710300000000, 1710300000000, 'Tòa L', 21.0385, 105.7475, 100, 'FAC001');

-- 11. Facility Shift (Ca học tại cơ sở)
INSERT INTO `facility_shift` (`id`, `status`, `created_at`, `updated_at`, `shift`, `from_hour`, `from_minute`, `to_hour`, `to_minute`, `id_facility`) VALUES
('SHF001', 1, 1710300000000, 1710300000000, 1, 7, 30, 9, 30, 'FAC001'),
('SHF002', 1, 1710300000000, 1710300000000, 2, 9, 30, 11, 30, 'FAC001'),
('SHF003', 1, 1710300000000, 1710300000000, 3, 13, 30, 15, 30, 'FAC001');

-- 12. Project (Dự án)
INSERT INTO `project` (`id`, `status`, `created_at`, `updated_at`, `name`, `description`, `id_level_project`, `id_subject_facility`, `id_semester`) VALUES
('PJ001', 1, 1710300000000, 1710300000000, 'Dự án Quản lý Bán hàng', 'Quản lý bán hàng cho shop thời trang', 'LVL002', 'SF002', 'SEM001'),
('PJ002', 1, 1710300000000, 1710300000000, 'Hệ thống Quản lý Thư viện', 'Quản lý đầu sách và mượn trả', 'LVL002', 'SF002', 'SEM001');

-- 13. Factory (Xưởng thực hành)
INSERT INTO `factory` (`id`, `status`, `created_at`, `updated_at`, `name`, `description`, `id_project`, `id_user_staff`) VALUES
('FT001', 1, 1710300000000, 1710300000000, 'Xưởng Phần mềm 1', 'Thực hành Java/Spring Boot', 'PJ001', 'STAFF002'),
('FT002', 1, 1710300000000, 1710300000000, 'Xưởng Di động 1', 'Thực hành Android/Flutter', 'PJ002', 'STAFF002');

-- 14. User Student (Sinh viên)
INSERT INTO `user_student` (`id`, `status`, `created_at`, `updated_at`, `email`, `name`, `code`, `image`, `face_embedding`, `id_facility`) VALUES
('STU001', 1, 1710300000000, 1710300000000, 'hungnvph12345@fpt.edu.vn', 'Nguyễn Văn Hùng', 'PH12345', NULL, NULL, 'FAC001'),
('STU002', 1, 1710300000000, 1710300000000, 'lannt@fpt.edu.vn', 'Nguyễn Thị Lan', 'PH12346', NULL, NULL, 'FAC001'),
('STU003', 1, 1710300000000, 1710300000000, 'namdh@fpt.edu.vn', 'Đặng Hoài Nam', 'PH12347', NULL, NULL, 'FAC001');

-- 15. User Student Factory (Sinh viên tham gia xưởng)
INSERT INTO `user_student_factory` (`id`, `status`, `created_at`, `updated_at`, `id_factory`, `id_user_student`) VALUES
('USF001', 1, 1710300000000, 1710300000000, 'FT001', 'STU001'),
('USF002', 1, 1710300000000, 1710300000000, 'FT001', 'STU002'),
('USF003', 1, 1710300000000, 1710300000000, 'FT002', 'STU003');

-- 16. Plan (Kế hoạch học tập)
INSERT INTO `plan` (`id`, `status`, `created_at`, `updated_at`, `name`, `description`, `from_date`, `to_date`, `max_late_arrival`, `id_project`) VALUES
('PLN001', 1, 1710300000000, 1710300000000, 'Kế hoạch học Java Spring', 'Học Spring Boot cơ bản đến nâng cao', 1704067200000, 1709251200000, 15, 'PJ001');

-- 17. Plan Factory (Xưởng trong kế hoạch)
INSERT INTO `plan_factory` (`id`, `status`, `created_at`, `updated_at`, `id_plan`, `id_factory`) VALUES
('PF001', 1, 1710300000000, 1710300000000, 'PLN001', 'FT001'),
('PF002', 1, 1710300000000, 1710300000000, 'PLN001', 'FT002');

-- 18. Plan Date (Buổi học cụ thể)
-- type: 0: OFFLINE, 1: ONLINE
-- required_*: 0: DISABLE, 1: ENABLE
INSERT INTO `plan_date` (`id`, `status`, `created_at`, `updated_at`, `description`, `start_date`, `end_date`, `shift`, `late_arrival`, `link`, `room`, `type`, `required_location`, `required_ip`, `required_checkin`, `required_checkout`, `id_plan_factory`, `id_user_staff`) VALUES
('PD001', 1, 1710300000000, 1710300000000, 'Buổi 1: Overview', 1710374400000, 1710381600000, '1,2', 15, 'https://meet.google.com/abc', 'P301', 0, 1, 1, 1, 1, 'PF001', 'STAFF002'),
('PD002', 1, 1710300000000, 1710300000000, 'Buổi 2: JPA/Hibernate', 1710547200000, 1710554400000, '1,2', 15, 'https://meet.google.com/def', 'P301', 0, 1, 1, 1, 1, 'PF001', 'STAFF002');

-- 19. Import Log (Nhật ký import)
INSERT INTO `import_log` (`id`, `status`, `created_at`, `updated_at`, `id_user`, `code`, `file_name`, `type`, `id_facility`) VALUES
('IL001', 1, 1710300000000, 1710300000000, 'STAFF003', 'IMP_STU_01', 'students.xlsx', 1, 'FAC001');

-- 20. Attendance Recovery (Khôi phục điểm danh/Phòng chờ)
INSERT INTO `attendance_recovery` (`id`, `status`, `created_at`, `updated_at`, `name`, `description`, `day`, `total_student`, `id_import_log`, `id_facility`) VALUES
('AR001', 1, 1710300000000, 1710300000000, 'Phòng chờ buổi 1', 'Dành cho sinh viên chưa điểm danh buổi 1', 1710374400000, 2, 'IL001', 'FAC001');

-- 21. Attendance (Điểm danh)
-- attendance_status: 0: NOTCHECKIN, 1: ABSENT, 2: CHECKIN, 3: PRESENT
INSERT INTO `attendance` (`id`, `status`, `created_at`, `updated_at`, `id_plan_date`, `id_user_student`, `attendance_status`, `late_checkin`, `late_checkout`, `id_attendance_recovery`) VALUES
('ATT001', 1, 1710300000000, 1710300000000, 'PD001', 'STU001', 3, 0, 0, NULL),
('ATT002', 1, 1710300000000, 1710300000000, 'PD001', 'STU002', 3, 10, 0, NULL),
('ATT003', 1, 1710300000000, 1710300000000, 'PD001', 'STU003', 1, NULL, NULL, 'AR001');

-- 22. Settings (Cấu hình)
-- key must be from SettingKeys enum names
INSERT INTO `settings` (`key`, `value`) VALUES
('SHIFT_MAX_LATE_ARRIVAL', '15'),
('ATTENDANCE_EARLY_CHECKIN', '30'),
('EXPIRATION_MINUTE_LOGIN', '1440');

-- 23. Notification (Thông báo)
INSERT INTO `notification` (`id`, `status`, `created_at`, `updated_at`, `id_user`, `type`, `data`) VALUES
('NOT001', 1, 1710300000000, 1710300000000, 'STU001', 0, '{"message": "Bạn có lịch học mới ngày mai"}');
