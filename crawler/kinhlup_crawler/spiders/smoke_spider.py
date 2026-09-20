import scrapy


class SmokeSpider(scrapy.Spider):
    """Infrastructure-only spider.

    This intentionally does not crawl HUST. T01 owns the real HUST spider,
    extraction contract, deduplication, and database ingestion.
    """

    name = "smoke"
    start_urls = ["https://example.com/"]

    def parse(self, response):
        yield {
            "url": response.url,
            "title": response.css("title::text").get(),
        }
