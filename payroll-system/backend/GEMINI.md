# Quy ước phát triển backend

## Thiết kế API
- Tuân thủ nghiêm ngặt nguyên tắc RESTful
- Sử dụng OpenAPI 3.0 (Swagger)
- Tất cả API phải validate đầu vào (`@Valid`, `@RequestBody`)
- Phản hồi lỗi theo định dạng thống nhất (global exception handler)
- luôn build lại sau khi thay đổi code để đảm bảo không có lỗi cú pháp

## Thao tác cơ sở dữ liệu
- Sử dụng Spring Data JPA / Hibernate
- Database: PostgreSQL
- Bao gồm xử lý lỗi trong tất cả truy vấn
- Ghi log cho các thao tác nhạy cảm
- Sử dụng `@Transactional` cho các thao tác phức tạp

