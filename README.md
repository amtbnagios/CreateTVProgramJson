# AmtbTV节目创建工具<br>
本软件用于为AmtbTV创建节目列表，并生成缩图。<br>
创建的节目列表文件为：json/tvprogram.txt<br>

## 本软件使用FFMpeg生成缩略图<br>
请安装FFMpeg软件，并加入运行路径中，可以通过输入ffmpeg在当前目录下运行。

## 简单使用<br>
在终端中输入命令 java -jar CreateTVProgramJson.jar<br>
本命令将从 www.amtb.tw/app/unicast2xml.asp 获取节目数据。并去除无视频的节目，新添加的节目生成缩图。<br>
为了提高效率，将忽略update文件夹中下列文件中指定的数据：<br>
* emptyMp4UrlsFile.txt
* errProgram.txt
* noUpdateIds.txt

## 其它功能<br>

### 多线程下载更新缩略图<br>
在终端中输入命令 java -jar CreateTVProgramJson.jar CreatePic<br>
本命令将会为update/server.txt中指定的每个服务器创建一个线程，下载数据并生成缩图节目中。<br>
下载的视频分片数据保存在ts目录下，生成的图片保存在pic目录下。<br>
若图片存在，则不再创建，若要更新，可以删除原数据。<br>

### 删除出错的节目<br>
在终端中输入命令 java -jar CreateTVProgramJson.jar RemoveErrProgram<br>
本命令将会删除update/errProgram.txt中指定节目<br>

### 输出所有节目<br>
在终端中输入命令 java -jar CreateTVProgramJson.jar PrintPrograms<br>
本命令将会打印输出所有节目信息<br>

### 检测节目是否有生成的缩图<br>
在终端中输入命令 java -jar CreateTVProgramJson.jar CheckProgramPic<br>
本命令将会检测每个节目对应的缩图是否存在，来更新节目的picCreated信息。若缩图不存在，在TV中会显示内置的默认图片。<br>

### 更新指定节目数据<br>
在终端中输入命令 java -jar CreateTVProgramJson.jar UpdateIds<br>
本命令将会更新 update/updateIds.txt 中指写的节目数据。<br>

## 上传节目数据<br>
生成的节目数据主要有 tvprogram.txt 和缩图，将生成的数据用FTP上传到服务器相应目录即可。<br>

## 相关网站<br>
官方网站：http://www.amtb.tw/<br>
AMTB安卓APP发布网站：http://amtb.sfzd5.com/<br>
问题反馈邮箱：do168@126.com<br>

## 版权<br>
Apache 2.0<br>
