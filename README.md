# 安卓磁链搜索下载播放

> 了解磁链原理和使用，资源搜索必备神器




## 效果图

![1](/screenshot1.png)



![2](/screenshot2.png)

## 磁力搜索

### 获取磁链

> 抓取网络资源接口返回磁链地址
>
> `source` 可选 `种子搜|磁力吧|BT兔子|idope|BTDB|BT4G|屌丝搜|AOYOSO`

```html
http://bt.xiandan.in/api/search?&source=种子搜&keyword=测试&page=1
```

### 返回JSON

```json
{
  "currentPage": 1,
  "currentSourceSite": "种子搜",
  "results": [
    {
      "magnet": "magnet:?xt=urn:btih:b9f4c386974037282b077e516eced7ae50e57b7f",
      "name": "[星火字幕组][填坑][beta测试版非正式版][名侦探柯南剧场版M19][业火的向日葵][1080P][10bit][简日附带假名].mkv ",
      "formatSize": "6.83 GB",
      "size": 7333656576,
      "count": "2018-06-03",
      "detailUrl": "http://www.zhongzijun.com/info-b9f4c386974037282b077e516eced7ae50e57b7f",
      "resolution": "1080P"
    },
    {
      "magnet": "magnet:?xt=urn:btih:71ed94a7e48585fddeb178b5cb88afaf8354c1b4",
      "name": "2018_05_16园博园迈腾测试 ",
      "formatSize": "62.31 MB",
      "size": 65336772,
      "count": "2018-05-28",
      "detailUrl": "http://www.zhongzijun.com/info-71ed94a7e48585fddeb178b5cb88afaf8354c1b4",
      "resolution": ""
    }
  ]
}
```

## 磁链下载

> 感谢开源项目
>
> [1]https://github.com/masterwok/simple-torrent-android
>
> [2]https://github.com/frostwire/frostwire-jlibtorrent
>
> [3]https://com.github.dueeeke.dkplayer



### 开始查找下载数据

```kotlin
val torrentSessionOptions = TorrentSessionOptions(downloadLocation = File(rootPath), onlyDownloadLargestFile = true, enableLogging = false, shouldStream = true)
torrentSession = TorrentSession(torrentSessionOptions)
torrentSession?.listener = object : TorrentSessionListener {
    ...
    //各个状态监听
    ...
}
```

### 下载完成播放器

> 下载到本地文件夹,直接读取播放即可

```kotlin
//xml
<com.dueeeke.videoplayer.player.IjkVideoView
        android:id="@+id/video_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

//kt
val controller = FullScreenController(this)
        val playerConfig = PlayerConfig.Builder()
                .usingSurfaceView() //启用SurfaceView显示视频，不调用默认使用TextureView
                .savingProgress() //保存播放进度
                .disableAudioFocus() //关闭AudioFocusChange监听
                .build()
        video_view.setPlayerConfig(playerConfig)
        video_view.setUrl(intent.getStringExtra("url"))
        video_view.title = intent.getStringExtra("title")
        video_view.setVideoController(controller)
        video_view.startFullScreen()
        video_view.start()
```



### 调用迅雷打开磁链

> 从tracker上获取不到的时候比较多, 所以还是直接调用迅雷打开比较快

```kotlin
private fun wakeThunder(link: String) {
    //AAlinkZZ 不用转thunder://xxx 可以直接让迅雷识别magnet
    var intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    intent.addCategory("android.intent.category.DEFAULT")
    startActivity(intent)

}
```



- 边下边播还未实现...

