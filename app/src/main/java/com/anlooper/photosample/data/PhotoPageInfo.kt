package com.anlooper.photosample.data

/**
 * Created by Tae-hwan on 7/22/16.
 */
data class PhotoPageInfo(val page: Int,
                         val pages: Int,
                         val perpage: Int,
                         val total: Int,
                         val photo: List<Photo>,
                         val stat: String)