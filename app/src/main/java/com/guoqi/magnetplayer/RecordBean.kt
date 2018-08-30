package com.guoqi.magnetplayer

import java.io.Serializable

/**
 * Created by GUOQI on 2017/12/29.
 */
class RecordBean : Serializable {

    var currentPage: String = ""
    var currentSourceSite: String = ""
    lateinit var results: ArrayList<Results>

    constructor()

    class Results : Serializable {
        /*
        "magnet": "magnet:?xt=urn:btih:b9f4c386974037282b077e516eced7ae50e57b7f",
        "name": "[星火字幕组][填坑][beta测试版非正式版][名侦探柯南剧场版M19][业火的向日葵][1080P][10bit][简日附带假名].mkv ",
        "formatSize": "6.83 GB",
        "size": 7333656576,
        "count": "2018-06-03",
        "detailUrl": "http://www.zhongzijun.com/info-b9f4c386974037282b077e516eced7ae50e57b7f",
        "resolution": "1080P"
        */
        var magnet: String = ""
        var name: String = ""
        var formatSize: String = ""
        var size: String = ""
        var count: String = ""
        var detailUrl: String = ""
        var resolution: String = ""

        constructor()
    }
}