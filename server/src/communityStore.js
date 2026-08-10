// 社区在线同步存储（轻量 JSON 文件）
// 用途：支持"无网络用本地 + 有网络同步线上"双模式。
// 原型阶段用 JSON 文件持久化；正式版应换数据库。
"use strict";

const fs = require("fs");
const path = require("path");

const DATA_DIR = path.join(__dirname, "..", "data");
const DATA_FILE = path.join(DATA_DIR, "community.json");

function readPosts() {
  try {
    if (fs.existsSync(DATA_FILE)) {
      const raw = fs.readFileSync(DATA_FILE, "utf-8");
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed.posts) ? parsed.posts : [];
    }
  } catch (err) {
    console.warn("[petpet-gateway] 社区数据读取失败:", err.message);
  }
  return [];
}

function writePosts(posts) {
  try {
    fs.mkdirSync(DATA_DIR, { recursive: true });
    fs.writeFileSync(DATA_FILE, JSON.stringify({ posts }, null, 2), "utf-8");
  } catch (err) {
    console.warn("[petpet-gateway] 社区数据写入失败:", err.message);
  }
}

// 列出动态（可选按 userId 过滤）
function listPosts(userId) {
  const posts = readPosts();
  return userId ? posts.filter((p) => p.authorId === userId) : posts;
}

// 新增动态；返回带服务端 id 的完整记录
function addPost({ authorId, author, text }) {
  const posts = readPosts();
  const post = {
    id: Date.now().toString(36) + Math.random().toString(36).slice(2, 8),
    authorId: authorId || "anonymous",
    author: author || "小猫朋友",
    text: String(text || "").slice(0, 500),
    time: new Date().toISOString(),
    likeCount: 0,
    comments: [],
  };
  posts.unshift(post);
  // 只保留最近 200 条，防止文件无限膨胀
  writePosts(posts.slice(0, 200));
  return post;
}

function toggleLike(postId) {
  const posts = readPosts();
  let liked = false;
  for (const p of posts) {
    if (p.id === postId) {
      p.likeCount = (p.likeCount || 0) + 1;
      liked = true;
      break;
    }
  }
  if (liked) writePosts(posts);
  return liked;
}

module.exports = { listPosts, addPost, toggleLike };
