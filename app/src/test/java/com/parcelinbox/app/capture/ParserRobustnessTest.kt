package com.parcelinbox.app.capture

import kotlin.random.Random
import org.junit.Assert.assertTrue
import org.junit.Test

class ParserRobustnessTest {
    @Test
    fun promotionNoiseNeverCreatesAParcelOrThrows() {
        val random = Random(307)
        repeat(1_000) { index ->
            val prefix = buildString {
                repeat(random.nextInt(2, 14)) { append(('A'.code + random.nextInt(26)).toChar()) }
            }
            val numeric = random.nextLong(100_000_000L, 9_999_999_999L)
            val snapshot = VisiblePageSnapshot(
                fullText = listOf(
                    "订单好物推荐 ${random.nextInt()}",
                    "包裹会员优惠码 $prefix$numeric",
                    if (index % 2 == 0) "待发货专区限时活动" else "物流会员专享 coupon"
                ),
                candidateBlocks = emptyList(),
                visibleNodeCount = random.nextInt(1, 900)
            )

            val outcome = ParcelScreenParser.parseSnapshot(
                sourcePackage = "com.taobao.taobao",
                sourceLabel = "淘宝",
                snapshot = snapshot,
                observedAt = index.toLong()
            )
            assertTrue("noise iteration $index created a parcel", outcome.parcels.isEmpty())
        }
    }
}
