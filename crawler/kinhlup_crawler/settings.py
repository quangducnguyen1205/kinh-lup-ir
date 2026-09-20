BOT_NAME = "kinhlup_crawler"

SPIDER_MODULES = ["kinhlup_crawler.spiders"]
NEWSPIDER_MODULE = "kinhlup_crawler.spiders"

ROBOTSTXT_OBEY = True
CONCURRENT_REQUESTS_PER_DOMAIN = 4
DOWNLOAD_DELAY = 0.25

LOG_LEVEL = "INFO"
FEED_EXPORT_ENCODING = "utf-8"
