package com.anlooper.photosample.data

import tech.thdev.base.model.BaseItem

/**
 * Created by Tae-hwan on 7/22/16.
 */
data class Photo(val id: String,
                 val owner: String,
                 val secret: String,
                 val server: String,
                 val farm: Long,
                 val title: String,
                 val ispublic: Long,
                 val isfriend: Long,
                 val isfamily: Long,
                 override val viewType: Int) : BaseItem {

    fun getImageUrl(): String {
        return String.format("https://farm%s.staticflickr.com/%s/%s_%s.jpg", farm, server, id, secret)
    }
}