// 每用户内存限流：userId → 时间戳滑动窗口
// 注意：单机内存实现，适合原型。多实例部署需换 Redis 等共享存储。

class RateLimiter {
  constructor({ perMinute = 20 } = {}) {
    this.perMinute = perMinute;
    this.windowMs = 60_000;
    this.buckets = new Map(); // userId -> number[]
  }

  // 返回 true = 放行；false = 被限流
  allow(userId) {
    const now = Date.now();
    const cutoff = now - this.windowMs;
    if (!this.buckets.has(userId)) {
      this.buckets.set(userId, []);
    }
    const timestamps = this.buckets.get(userId).filter((t) => t > cutoff);
    if (timestamps.length >= this.perMinute) {
      this.buckets.set(userId, timestamps);
      return false;
    }
    timestamps.push(now);
    this.buckets.set(userId, timestamps);
    return true;
  }
}

module.exports = { RateLimiter };
