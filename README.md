# MarkCarame2
界面完全模仿微信朋友圈录制视频界面，用户可以自行在界面覆盖更多操作控件，例如：闪光灯，静音拍摄等。

### 集成方式
    目前lastversion;1.2.3,历史版本见release
    dependencies {
      implementation 'com.mark:markcamera:lastversion'
    }


### 混淆规则
    -dontwarn com.aliyun.**
    -dontwarn com.alibaba.sdk.android.vod.upload.**
    -dontwarn com.duanqu.**
    -dontwarn com.qu.**
    -dontwarn component.alive.com.facearengine.**
    -dontwarn com.alivc.component.encoder.**
    -keepattributes *Annotation*

    -keep class com.aliyun.** {*;}
    -keep class com.alibaba.sdk.android.vod.upload.** {*;}
    -keep class com.duanqu.** {*;}
    -keep class com.qu.** {*;}
    -keep class component.alive.com.facearengine.** {*;}
    -keep class com.alivc.component.encoder.** {*;}

    -keep public class **.R$*{
       public static final int *;
    }

## License

    The MVVM-Rhine: Apache License

    Copyright (c) 2018 qingmei2

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
