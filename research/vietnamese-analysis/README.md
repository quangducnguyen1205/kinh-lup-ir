# Phân tích văn bản tiếng Việt

**Người phụ trách setup:** Nam  
**Task khởi đầu:** T06 — Vietnamese text analysis study

## Mục tiêu ban đầu

- benchmark VnCoreNLP, Underthesea và PyVi trên dữ liệu HUST thực tế;
- chốt cách chuẩn hoá Unicode, chữ hoa/thường, khoảng trắng và dấu câu;
- chốt chiến lược tách từ;
- tạo test case có input và expected tokens;
- đảm bảo document và query dùng cùng chiến lược khi tích hợp với Lucene.

## Deliverable yêu cầu

Deliverable phải có code/demo callable hoặc test fixture, không chỉ có tài liệu nghiên cứu.

Step 5 mới cung cấp `benchmark.py` làm entry point; dữ liệu benchmark thật và kết luận lựa chọn thư viện vẫn thuộc T06.
