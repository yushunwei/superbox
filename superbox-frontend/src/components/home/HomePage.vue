<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { List, Collection, ChatDotRound, PriceTag } from '@element-plus/icons-vue'
import { apps, configApps } from '@/apps'
import { useUserStore } from '@/stores/modules/user'

const iconMap: Record<string, any> = { List, Collection, ChatDotRound, PriceTag }

const { t } = useI18n()
const router = useRouter()
const userStore = useUserStore()

function goTo(route: string) {
  router.push(route)
}
</script>

<template>
  <div class="home-page">
    <!-- Top bar -->
    <div class="top-bar">
      <div></div>
      <el-button text size="small" class="logout-btn" @click="userStore.logout()">
        退出
      </el-button>
    </div>

    <!-- Header -->
    <div class="home-header">
      <h1>{{ t('app.title') }}</h1>
      <p>{{ t('app.subtitle') }}</p>
    </div>

    <!-- App Cards -->
    <div class="section-header">{{ t('home.myApps') }}</div>
    <div class="app-grid">
      <div
        v-for="app in apps"
        :key="app.id"
        :class="['app-card', `app-card--${app.id}`]"
        @click="goTo(app.route)"
      >
        <div class="app-card__icon">
          <el-icon :size="28"><component :is="iconMap[app.icon]" /></el-icon>
        </div>
        <div class="app-card__info">
          <div class="app-card__title">{{ app.label }}</div>
          <div class="app-card__desc">{{ app.desc || '' }}</div>
        </div>
        <div class="app-card__arrow">
          <span>→</span>
        </div>
      </div>
    </div>

    <!-- Config Section -->
    <div class="section-header">配置管理</div>
    <div class="app-grid">
      <div
        v-for="app in configApps"
        :key="app.id"
        :class="['app-card', `app-card--${app.id}`]"
        @click="goTo(app.route)"
      >
        <div class="app-card__icon">
          <el-icon :size="28"><component :is="iconMap[app.icon]" /></el-icon>
        </div>
        <div class="app-card__info">
          <div class="app-card__title">{{ app.label }}</div>
          <div class="app-card__desc">{{ app.desc || '' }}</div>
        </div>
        <div class="app-card__arrow">
          <span>→</span>
        </div>
      </div>
    </div>

    <!-- Footer -->
    <div class="home-footer">
      <span>Superbox · 个人高效工作台</span>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  max-width: 680px;
  margin: 0 auto;
  padding: 32px 24px 48px;
  min-height: 100vh;
}

/* Top bar */
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40px;
}
.logout-btn {
  color: #94a3b8;
  font-size: 13px;
}
.logout-btn:hover {
  color: #64748b;
}

/* Header */
.home-header {
  text-align: center;
  margin-bottom: 48px;
}
.home-header h1 {
  font-size: 32px;
  font-weight: 800;
  color: #1e293b;
  letter-spacing: -0.5px;
  margin-bottom: 6px;
}
.home-header p {
  font-size: 15px;
  color: #64748b;
  font-weight: 400;
}

/* Section */
.section-header {
  font-size: 12px;
  font-weight: 600;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-bottom: 14px;
  padding-left: 4px;
}

/* App Cards */
.app-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 48px;
}

.app-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 20px 24px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.app-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(37, 99, 235, 0.10), 0 2px 8px rgba(0, 0, 0, 0.06);
  border-color: #bfdbfe;
}

.app-card__icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.app-card--taskManager .app-card__icon {
  background: #eff6ff;
  color: #3b82f6;
}
.app-card--knowledgeBase .app-card__icon {
  background: #f5f3ff;
  color: #7c3aed;
}
.app-card--aiChat .app-card__icon {
  background: #ecfdf5;
  color: #10b981;
}
.app-card--tagManager .app-card__icon {
  background: #fff7ed;
  color: #f97316;
}

.app-card__info {
  flex: 1;
  min-width: 0;
}
.app-card__title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 3px;
}
.app-card__desc {
  font-size: 13px;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-card__arrow {
  flex-shrink: 0;
  color: #cbd5e1;
  font-size: 18px;
  transition: transform 0.2s, color 0.2s;
}
.app-card:hover .app-card__arrow {
  transform: translateX(3px);
  color: #3b82f6;
}

/* Footer */
.home-footer {
  text-align: center;
  font-size: 12px;
  color: #cbd5e1;
}
</style>
