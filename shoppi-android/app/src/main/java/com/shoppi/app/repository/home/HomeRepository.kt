package com.shoppi.app.repository.home

import com.shoppi.app.model.HomeData

/**
 * @author jihoon
 * @email dlwlgns1240@gmail.com
 * @created 2022/08/28
 * @desc
 */

class HomeRepository(
    private val assetDataSource: HomeAssetDataSource
) {
    suspend fun getHomeData(): HomeData? {
        return assetDataSource.getHomeData()
    }
}