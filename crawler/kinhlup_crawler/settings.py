import os
from pathlib import Path

from dotenv import load_dotenv

# The shared .env lives at the repository root. Variables already exported in
# the shell take precedence over it.
load_dotenv(Path(__file__).resolve().parents[2] / ".env")

BOT_NAME = "kinhlup_crawler"

SPIDER_MODULES = ["kinhlup_crawler.spiders"]
NEWSPIDER_MODULE = "kinhlup_crawler.spiders"

CRAWLER_START_URL = os.getenv("CRAWLER_START_URL", "https://hust.edu.vn/")
CRAWLER_ALLOWED_DOMAIN = os.getenv("CRAWLER_ALLOWED_DOMAIN", "hust.edu.vn")

# Safety cap for local runs: the crawl stops after this many responses
# (robots.txt, redirects and errors included); requests already in flight
# still finish, so a run can end slightly above it. 0 removes the cap.
CLOSESPIDER_PAGECOUNT = int(os.getenv("CRAWLER_MAX_PAGES", "50"))

# Polite crawling of university servers: public GET requests only, no cookies
# or sessions, and a slow, adaptive request rate per host.
USER_AGENT = "KinhLupBot/0.1 (+https://github.com/quangducnguyen1205/kinh-lup-ir; IT4863 student project)"
ROBOTSTXT_OBEY = True
COOKIES_ENABLED = False
CONCURRENT_REQUESTS = 8
CONCURRENT_REQUESTS_PER_DOMAIN = 2
DOWNLOAD_DELAY = 1.0
AUTOTHROTTLE_ENABLED = True
AUTOTHROTTLE_START_DELAY = 1.0
AUTOTHROTTLE_TARGET_CONCURRENCY = 1.0
DOWNLOAD_TIMEOUT = 30
DOWNLOAD_MAXSIZE = 10 * 1024 * 1024

# Breadth-first order, so a capped crawl covers the top pages of many sites
# instead of going deep into one of them.
DEPTH_PRIORITY = 1
SCHEDULER_DISK_QUEUE = "scrapy.squeues.PickleFifoDiskQueue"
SCHEDULER_MEMORY_QUEUE = "scrapy.squeues.FifoMemoryQueue"

# Local debugging consoles are not needed and would open extra local ports.
TELNETCONSOLE_ENABLED = False
REMOTE_CONTROL_ENABLED = False

LOG_LEVEL = "INFO"
FEED_EXPORT_ENCODING = "utf-8"
