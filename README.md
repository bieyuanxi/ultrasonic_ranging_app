# 外部依赖
项目使用了[rustfft](https://docs.rs/rustfft/latest/rustfft/)加速fft和ifft，
使用[rust_fft_wrapper](https://gitee.com/bieyuanxi/rust_fft_wrapper.git)简要包装并编译依赖库
## 使用简单包装的rustfft库
可直接下载已经编译好的库：
1. 创建文件夹：app/src/main/jniLibs
2. 将[压缩文件](https://pan.baidu.com/s/1jw0eY33CJlsTDmH_7J28Sw?pwd=mfxf)解压到jniLibs目录下
3. 库文件按照如下表示存放：
    ```shell
    ├───jniLibs
    │   ├───arm64-v8a
    │   │       librust_fft_wrapper.so
    │   │
    │   ├───armeabi-v7a
    │   │       librust_fft_wrapper.so
    │   │
    │   └───x86_64
    │           librust_fft_wrapper.so
    ```
4. 编译Android项目时应该不会出现编译错误了


# 问题记录
1. 两台设备发送/接收到的信号强度不同
2. 奇载波、偶载波问题
3. 发送声波时有可听到的声音（因设备而异）
4. 相位偏移问题
5. 设备自身扬声器与麦克风距离未知问题
6. 远距离可靠性问题
