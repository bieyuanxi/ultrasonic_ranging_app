# 使用简单包装的rustfft库
1. 创建文件夹：app/src/main/jniLibs
2. 将[压缩文件](https://pan.baidu.com/s/1jw0eY33CJlsTDmH_7J28Sw?pwd=mfxf)解压到jniLibs目录下
3. 你应该能看到：
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
4. Done


# 问题记录
1. 两台设备发送/接收到的信号强度不同
2. 奇载波、偶载波问题
3. 发送声波时有可听到的声音（因设备而异）
4. 相位偏移问题
5. 设备自身扬声器与麦克风距离未知问题
6. 远距离可靠性问题
