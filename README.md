# Milktea Backend

Đây là ứng dụng backend được xây dựng trên nền tảng **Spring Boot** dành cho hệ thống quản lý cửa hàng trà sữa. Ứng dụng cung cấp các API để quản lý menu, đặt hàng, quản lý giỏ hàng, thanh toán và quản trị hệ thống.

## Các tính năng chính

- **Quản lý người dùng:** Đăng ký, đăng nhập (JWT), xác thực email.
- **Quản lý sản phẩm:** Danh mục, sản phẩm, kích thước (size), topping, chương trình khuyến mãi.
- **Quản lý giỏ hàng:** Thêm/xóa sản phẩm, cập nhật số lượng, đồng bộ giỏ hàng.
- **Quản lý đơn hàng:** Đặt hàng, theo dõi lịch sử đơn hàng, cập nhật trạng thái đơn hàng (dành cho Admin).
- **Thanh toán:** Xử lý các giao dịch thanh toán.
- **Admin Dashboard:** Tổng quan thống kê và quản lý toàn bộ hệ thống.

## Công nghệ sử dụng

- **Framework:** Spring Boot 4.0.5
- **Ngôn ngữ:** Java 17
- **Database:** MySQL
- **ORM:** Spring Data JPA / Hibernate
- **Security:** Spring Security, JSON Web Token (JWT)
- **Công cụ hỗ trợ:** Lombok, Spring Mail, Apache POI (Excel/CSV), Apache PDFBox.
- **Khác:** Spring Scheduling, Async Tasks.

## Yêu cầu hệ thống

- Java Development Kit (JDK) 17 hoặc cao hơn.
- Apache Maven.
- MySQL Server.

## Cấu hình và Chạy dự án

### 1. Cấu hình database
Tạo database MySQL và cập nhật thông tin kết nối trong tệp `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/milktea_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 2. Build
Chạy lệnh sau tại thư mục gốc của dự án để build:

```bash
mvn clean install
```

### 3. Chạy ứng dụng
Chạy ứng dụng bằng lệnh:

```bash
mvn spring-boot:run
```

Ứng dụng sẽ mặc định khởi chạy tại `http://localhost:8080`.

## Cấu trúc thư mục

```text
src/main/java/com/example/milktea_backend/
├── configs      # Cấu hình hệ thống (Security, Database, etc.)
├── controllers  # API Endpoints
├── dtos         # Data Transfer Objects (Request/Response)
├── entities     # JPA Database Entities
├── enums        # Các kiểu liệt kê (Enum)
├── exceptions   # Xử lý ngoại lệ toàn cục
├── repositories # Data Access Layer
├── security     # Cấu hình bảo mật và JWT
├── services     # Business Logic
└── utils        # Các tiện ích chung
```
