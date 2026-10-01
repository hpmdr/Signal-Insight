# SignalInsight ProGuard / R8 规则
#
# 现状：本应用不使用反射、序列化框架或动态资源查找，R8 裁剪本身是安全的
# （已核对：全项目无 Class.forName / getDeclaredMethod / newInstance / getIdentifier；
#   两个 ViewModel 均通过显式 Factory 构造；Compose / DataStore / TelephonyManager
#   的用法不需要额外 keep 规则）。
#
# 因此这里只做一件事：保留行号信息，让 release 崩溃堆栈可读。
# 否则用户反馈的崩溃栈只有类名和方法名、没有行号，定位成本极高。

-keepattributes SourceFile,LineNumberTable

# 隐藏原始源文件名（堆栈中显示为 SourceFile 而非真实文件名）：
# 与上一行配合，既保留行号可定位，又不暴露内部文件命名。
-renamesourcefileattribute SourceFile
