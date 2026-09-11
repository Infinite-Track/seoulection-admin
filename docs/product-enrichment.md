# 제품 정보 보완

관리자 사이드바 **제품 정보 보완** 또는 `/admin/products/enrichment`에서 접근한다.
`status = NEED_PRODUCT_INFO`인 제품만 25개씩 조회한다. 다른 필드의 null 여부는 조건에 포함하지 않는다. ASIN 부분 검색과 ID 정확 검색을 지원한다.

제품명, 브랜드, 카테고리, 설명, 가격, 제휴링크, 사진을 입력한다. 한글 제품명은 선택이며 빈 입력은 null로 저장한다. 가격은 0 이상, 정수 10자리/소수 2자리 이내이며 MongoDB Decimal128로 저장한다. 저장 시 status는 항상 PENDING으로 변경한다. 통계 메타데이터는 보존한다.

JPG/PNG 파일(10MB, 4천만 픽셀 이하)을 선택하면 JPG로 변환하고 `S3_PREFIX/ASIN.jpg`에 업로드한다. PNG 투명 배경은 흰색으로 변환한다. 반환된 S3/CloudFront URL을 thumbnail_url에 저장한다. 제휴링크는 MongoDB product_url에 저장한다.


기존 `crawlers/thumbnails/s3_uploader.py`의 업로드 규칙을 관리자 Java 런타임으로 이식했다. Python 프로세스를 실행하지 않으며 동일한 환경 변수와 AWS 기본 자격 증명 체인을 사용한다.

| 환경 변수 | 용도 |
| --- | --- |
| `S3_BUCKET` | 기존 이미지 버킷, 필수 |
| `AWS_REGION` | 버킷 리전. 미설정 시 SDK 기본 리전 체인 |
| `S3_PREFIX` | 기본 `product-pictures` |
| `CLOUDFRONT_DOMAIN` | 선택. 프로토콜 없는 CDN 도메인. 미설정 시 S3 URL |
| AWS 자격 증명 | 배포 IAM 역할 권장. 기존 AWS 환경 변수 또는 `AWS_PROFILE`도 사용 가능 |

배포 시 위 환경 변수를 **admin 컨테이너에도 전달**해야 한다. 다른 서비스나 로컬 Python `.env`에만 있으면 관리자에 자동 적용되지 않는다. 해당 prefix에 `s3:PutObject` 권한과 기존 이미지 조회 정책이 필요하다. 공개 ACL은 설정하지 않는다. 동일 ASIN의 사진은 같은 키에 업로드되므로 기존 객체를 교체한다.

S3 업로드 실패 시 MongoDB는 수정하지 않고 텍스트 입력을 유지한다. 브라우저 보안상 재시도 때 파일을 다시 선택해야 한다. MongoDB 갱신은 NEED_PRODUCT_INFO 상태와 ID를 함께 검사하고 `$set`으로 입력 필드와 상태를 수정한다. 다른 관리자가 먼저 채웠으면 덮어쓰지 않는다. S3와 MongoDB는 단일 트랜잭션이 아니므로 업로드 후 DB 실패/동시 수정 시 미참조 객체가 남을 수 있다. DB의 쓰기 결과가 불확실한 경우 정상 이미지 삭제를 피하기 위해 자동 삭제하지 않는다.

검증: `./gradlew test --tests '*ProductEnrichment*' --tests '*ProductImage*'` (MongoDB 통합 테스트는 Docker 필요).

## 기존 정보 보존

null, 누락, 빈 문자열/공백인 항목만 입력칸을 표시한다. 기존 값은 읽기 전용이며 가격 0도 기존 값으로 취급한다. 사진이 있으면 파일 업로드가 필요 없다. 서버는 화면에서 숨긴 필드가 POST로 전달되어도 기존 DB 값을 유지한다. 저장 직전에 문서를 다시 조회하고 빈 항목만 갱신하며, 조회 이후 값이 변경되면 조건부 갱신을 중단한다. 필수 값이 모두 채워져야 PENDING으로 전환한다. 한글명은 선택이다.
