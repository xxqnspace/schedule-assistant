# 混淆规则（release 构建开启 minify 后生效）

# Room：实体/DAO/数据库模型必须保留（KSP 生成代码 + 反射）
-keep class com.scheduleassistant.app.data.** { *; }
-keep class com.scheduleassistant.app.data.model.** { *; }

# 保留注解信息（Room/Compose 生成代码需要）
-keepattributes *Annotation*

# org.json 无反射，无需 keep
# Compose 编译器已处理自身保留规则，无需额外配置
