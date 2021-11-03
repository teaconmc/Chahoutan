# Chahoutan

The backend of contents in Chahoutan.

茶后谈内容的后端。

## 构建和使用

需要 Java 17 或更高版本。

Linux / macOS：

```shell
./gradlew bootJar

java -jar build/libs/Chahoutan-<version>.jar
```

Windows：

```shell
gradlew.bat bootJar

java -jar build/libs/Chahoutan-<version>.jar
```

工作目录文件：

* `chahoutan-indexes` 目录为茶后谈文本索引。
* `chahoutan.sqlite3.db` 文件为茶后谈 SQLite 数据库。
* `chahoutan-spring.log` 文件为 Spring Boot 运行日志。
* `chahoutan-tokens.txt` 文件为私有 API 鉴权 token 列表。

## 数据库格式

```sql
create table chahoutan_editors
(
    post_id integer not null,
    editor text not null,
    primary key (post_id, editor)
);

create table chahoutan_image_binaries
(
    image_id varchar(64) not null,
    binary blob not null,
    suffix varchar(16) not null,
    primary key (image_id, suffix)
);

create table chahoutan_image_sizes
(
    image_id varchar(64) not null primary key,
    height integer not null,
    width integer not null
);

create table chahoutan_images
(
    id varchar(64) not null primary key,
    upload_time datetime not null
);

create table chahoutan_post_anchors
(
    revision_id blob not null,
    link text not null,
    anchor text not null,
    primary key (revision_id, anchor)
);

create table chahoutan_post_images
(
    revision_id blob not null,
    image_id varchar(64) not null,
    image_ordinal integer not null,
    primary key (revision_id, image_ordinal)
);

create table chahoutan_posts
(
    id integer not null primary key,
    revision_id blob -- nullable
);

create table chahoutan_revisions
(
    id blob not null primary key,
    creation_time datetime not null,
    text text not null,
    post integer not null
);
```

## HTTP API 格式

HTTP 后端位于 48175 端口。

参数说明：

* `{{query}}` 为检索字符串，用于检索茶后谈。
* `{{post-id}}` 为正整数，代表茶后谈的期数。
* `{{revision-id}}` 为 UUID，代表茶后谈的一次修订。
* `{{image-id}}` 为图片索引，为 128 bit 长（64 字符）的十六进制。

公开访问 API：

```text
# 显示最新 API 版本
GET /

# RSS / Atom
GET /feed
GET /feed/atom

# 查看重新索引已有茶后谈的状态
GET /v1/refresh

# 获取图片 metadata / png 格式 / webp 格式
GET /v1/images/{{image-id}}
GET /v1/images/{{image-id}}.png
GET /v1/images/{{image-id}}.webp

# 查看或检索已公开的茶后谈 / 查看某次茶后谈修订
GET /v1/posts
GET /v1/posts?q={{query}}
GET /v1/posts/{{post-id}}
GET /v1/posts/{{post-id}}/images
GET /v1/posts/{{post-id}}/editors
GET /v1/posts/{{revision-id}}
GET /v1/posts/{{revision-id}}/images
```

私有访问 API：

> 需添加 `Authorization: Bearer <token>` 请求头，`<token>` 位于 `chahoutan-tokens.txt` 下。

```text
# 获取图片被哪些茶后谈引用
GET /v1/images/{{image-id}}/revisions

# 获取当前茶后谈的所有修订
GET /v1/posts/{{post-id}}/revisions
GET /v1/posts/{{revision-id}}/revisions
```

私有更新 API：

> 需添加 `Authorization: Bearer <token>` 请求头，`<token>` 位于 `chahoutan-tokens.txt` 下。

> `POST /v1/images` 的 `Content-Type` 需为图片格式，`DELETE` 和 `POST /v1/refresh` 无需 `Content-Type`，其他均为 `application/json`。

```text
# 重新索引已有茶后谈（无需 Content-Type，body 将会被忽略）
POST /v1/refresh

# 上传图片（Content-Type 需为图片格式）
POST /v1/images

# 删除图片（已被某版修订引用的图片无法删除）
DELETE /v1/images/{{image-id}}

# 上传新的茶后谈（创建新修订）
POST /v1/posts

# 删除已有茶后谈（已有修订仍可检索）
DELETE /v1/posts/{{post-id}}

# 修改已有茶后谈的图片（创建新修订）
PUT /v1/posts/{{post-id}}/images

# 修改已有茶后谈的编辑者（历史记录不会保存）
PUT /v1/posts/{{post-id}}/editors
```
