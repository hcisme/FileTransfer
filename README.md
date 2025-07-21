# 局域网内文件传输工具

## Node 端使用指南

### 安装依赖
```bash
npm install
```

### 启动应用
```bash
npm start
```

### 构建可执行文件
```bash
npm run build
```

> 📁 **默认下载目录**：  
> `C:\Users\<用户名>\Downloads`  
> (请将 `<用户名>` 替换为您的实际用户名)

---

## Android 端使用指南
- 使用 Android Studio 打开项目
- 连接 Android 物理设备
- 点击运行按钮 ▶️ 部署应用

---

## 文件类型与存储目录映射表

系统会根据文件扩展名自动分类存储到对应目录：

| 文件类型   | 包含的扩展名                                  | 目标目录            | 系统路径示例                  |
|------------|----------------------------------------------|---------------------|-----------------------------|
| **图片**   | `jpg`, `jpeg`, `png`, `gif`, `webp`, `bmp`   | `PICTURES`          | `~/Pictures/`              |
| **音频**   | `mp3`, `wav`, `flac`, `ogg`, `m4a`           | `MUSIC`             | `~/Music/`                 |
| **视频**   | `mp4`, `mkv`, `avi`, `mov`, `flv`            | `MOVIES`            | `~/Videos/`                |
| **文档**   | `pdf`, `doc/docx`, `xls/xlsx`, `ppt/pptx`, `txt` | `DOCUMENTS`       | `~/Documents/`             |
| **APK**    | `apk`                                        | `DOWNLOADS`         | `~/Downloads/`             |
| **其他**   | 所有未列出的扩展名 (如 `.zip`, `.rar` 等)     | `DOWNLOADS`         | `~/Downloads/`             |
