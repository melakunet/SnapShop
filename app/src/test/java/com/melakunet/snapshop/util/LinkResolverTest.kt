package com.melakunet.snapshop.util

import com.melakunet.snapshop.models.ShopItem
import org.junit.Test
import org.junit.Assert.*

class LinkResolverTest {

    private fun createItem(source: String, link: String, title: String? = null) = ShopItem(
        price = "$10",
        extractedPrice = 10.0,
        delivery = "Free",
        source = source,
        link = link,
        thumbnail = "",
        title = title
    )

    @Test
    fun testDirectLinkPassthrough() {
        val item = createItem("Amazon", "https://amazon.com/dp/123")
        assertEquals("https://amazon.com/dp/123", LinkResolver.resolveBestUrl(item, "query"))
    }

    @Test
    fun testAmazonMapping() {
        val item = createItem("Amazon", "https://google.com/search?q=...", "Sony WH-1000XM5")
        assertEquals("https://www.amazon.com/s?k=Sony+WH-1000XM5", LinkResolver.resolveBestUrl(item, "query"))
    }

    @Test
    fun testWalmartMarketplaceMapping() {
        val item = createItem("Walmart - TopShoes", "https://google.com/search?q=...", "Running Shoes")
        assertEquals("https://www.walmart.com/search?q=Running+Shoes", LinkResolver.resolveBestUrl(item, "query"))
    }

    @Test
    fun testTargetMapping() {
        val item = createItem("Target", "https://google.com/search?q=...", "Coffee Maker")
        assertEquals("https://www.target.com/s?searchTerm=Coffee+Maker", LinkResolver.resolveBestUrl(item, "query"))
    }

    @Test
    fun testBestBuyMapping() {
        val item = createItem("Best Buy", "https://google.com/search?q=...", "iPad Pro")
        assertEquals("https://www.bestbuy.com/site/searchpage.jsp?st=iPad+Pro", LinkResolver.resolveBestUrl(item, "query"))
    }

    @Test
    fun testHomeDepotMapping() {
        val item = createItem("Home Depot", "https://google.com/search?q=...", "Drill")
        assertEquals("https://www.homedepot.com/s/Drill", LinkResolver.resolveBestUrl(item, "query"))
    }

    @Test
    fun testEbayMapping() {
        val item = createItem("eBay", "https://google.com/search?q=...", "Vintage Watch")
        assertEquals("https://www.ebay.com/sch/i.html?_nkw=Vintage+Watch", LinkResolver.resolveBestUrl(item, "query"))
    }

    @Test
    fun testNeweggMapping() {
        val item = createItem("Newegg", "https://google.com/search?q=...", "RTX 4090")
        assertEquals("https://www.newegg.com/p/pl?d=RTX+4090", LinkResolver.resolveBestUrl(item, "query"))
    }

    @Test
    fun testFallbackToQuery() {
        val item = createItem("Amazon", "https://google.com/search?q=...", null)
        assertEquals("https://www.amazon.com/s?k=original+query", LinkResolver.resolveBestUrl(item, "original query"))
    }

    @Test
    fun testUnknownSourceFallback() {
        val item = createItem("Local Store", "https://google.com/search?q=...", "Item")
        assertEquals("https://google.com/search?q=...", LinkResolver.resolveBestUrl(item, "query"))
    }

    @Test
    fun testUrlEncoding() {
        val item = createItem("Amazon", "https://google.com/search?q=...", "Item & Stuff")
        assertEquals("https://www.amazon.com/s?k=Item+%26+Stuff", LinkResolver.resolveBestUrl(item, "query"))
    }
}
