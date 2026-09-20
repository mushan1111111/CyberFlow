<!--
  SiteOrderRankingPage - 站点订单排行页面
  按站点维度统计指定时间段内的订单表现，默认当月 1 号至今天。
  支持按域名、负责人、主题、订单数、金额、收录数与商品数做组合筛选，
  所有列均可点击表头排序（服务端排序）。
-->
<template>
  <el-card>
    <el-form :inline="true" :model="filters" class="ranking-filter" @submit.prevent="handleSearch">
      <el-form-item v-if="isAdmin" label="用户组">
        <el-segmented v-model="filters.userGroup" :options="groupOptions" @change="handleSearch" />
      </el-form-item>
      <el-form-item label="订单日期" class="date-filter">
        <el-date-picker
          v-model="filters.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
      <el-form-item label="站点域名">
        <el-input v-model="filters.domain" placeholder="输入域名关键词" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="负责人">
        <el-input v-model="filters.adminName" placeholder="输入负责人" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="主题">
        <el-input v-model="filters.themeName" placeholder="输入主题" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="去重订单 ≥">
        <el-input-number v-model="filters.minOrders" :min="0" :controls="false" placeholder="不限" class="mini-input" />
      </el-form-item>
      <el-form-item label="成功订单 ≥">
        <el-input-number v-model="filters.minSuccessfulOrders" :min="0" :controls="false" placeholder="不限" class="mini-input" />
      </el-form-item>
      <el-form-item label="总订单金额 ≥">
        <el-input-number v-model="filters.minTotalAmount" :min="0" :controls="false" placeholder="不限" class="mini-input" />
      </el-form-item>
      <el-form-item label="成功金额 ≥">
        <el-input-number v-model="filters.minSuccessfulAmount" :min="0" :controls="false" placeholder="不限" class="mini-input" />
      </el-form-item>
      <el-form-item label="Google收录 ≥">
        <el-input-number v-model="filters.minIndexCount" :min="0" :controls="false" placeholder="不限" class="mini-input" />
      </el-form-item>
      <el-form-item label="商品数 ≥">
        <el-input-number v-model="filters.minProductCount" :min="0" :controls="false" placeholder="不限" class="mini-input" />
      </el-form-item>
      <el-form-item class="filter-actions">
        <el-button native-type="submit" type="primary">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="ranking-summary">
      <article v-for="item in summaryItems" :key="item.label" class="summary-item" :class="item.tone">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.note }}</small>
      </article>
    </div>

    <el-table
      :data="tableData"
      v-loading="loading"
      stripe
      class="ranking-table"
      @sort-change="handleSortChange"
    >
      <el-table-column type="index" label="#" width="54" />
      <el-table-column prop="site_domain" label="购买网站" min-width="180" show-overflow-tooltip sortable="custom" />
      <el-table-column label="负责人" min-width="125" sortable="custom" prop="admin_name" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.real_name && row.real_name !== row.admin_name ? `${row.admin_name}（${row.real_name}）` : row.admin_name }}
        </template>
      </el-table-column>
      <el-table-column label="组别" width="80" align="center">
        <template #default="{ row }"><el-tag v-if="row.user_group" effect="light">{{ row.user_group }}</el-tag><span v-else>—</span></template>
      </el-table-column>
      <el-table-column prop="add_date" label="建站日期" width="110" sortable="custom" />
      <el-table-column prop="theme_name" label="主题" min-width="110" show-overflow-tooltip />
      <el-table-column prop="index_count" label="Google收录" width="110" align="right" sortable="custom" />
      <el-table-column prop="product_count" label="商品数" width="95" align="right" sortable="custom" />
      <el-table-column prop="total_orders" label="总订单" width="95" align="right" sortable="custom" />
      <el-table-column label="总订单金额" min-width="120" align="right" sortable="custom" prop="total_amount">
        <template #default="{ row }">{{ formatMoney(row.total_amount) }}</template>
      </el-table-column>
      <el-table-column prop="deduplicated_orders" label="去重订单" width="100" align="right" sortable="custom" />
      <el-table-column prop="successful_orders" label="成功订单" width="100" align="right" sortable="custom" />
      <el-table-column label="成功金额" min-width="120" align="right" sortable="custom" prop="successful_amount">
        <template #default="{ row }">{{ formatMoney(row.successful_amount) }}</template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top: 16px; justify-content: flex-end;"
      v-model:current-page="page"
      :page-size="size"
      :page-sizes="[20, 50, 100, 200]"
      :total="total"
      layout="total, sizes, prev, pager, next"
      @current-change="fetchData"
      @size-change="handleSizeChange"
    />
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getSiteOrderRanking } from '@/api/dashboard'
import { useUserStore } from '@/store/user'
import { useSiteGroups } from '@/composables/useSiteGroups'

const userStore = useUserStore()
const { groupOptions, loadSiteGroups } = useSiteGroups()
const isAdmin = computed(() => (userStore.userInfo?.roles || []).some(role => String(role).toUpperCase() === 'ROLE_ADMIN'))
const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const size = ref(20)
const total = ref(0)
const summary = ref({})
const sortBy = ref('deduplicated_orders')
const sortDir = ref('desc')
const dateKey = date => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
const currentMonthRange = () => {
  const today = new Date()
  const first = new Date(today.getFullYear(), today.getMonth(), 1)
  return [dateKey(first), dateKey(today)]
}
const defaultFilters = () => ({
  userGroup: '',
  dateRange: currentMonthRange(),
  domain: '',
  adminName: '',
  themeName: '',
  minOrders: undefined,
  minSuccessfulOrders: undefined,
  minTotalAmount: undefined,
  minSuccessfulAmount: undefined,
  minIndexCount: undefined,
  minProductCount: undefined,
})
const filters = reactive(defaultFilters())
const toNumber = value => Number(value || 0)
const formatNumber = value => value === undefined || value === null ? '—' : toNumber(value).toLocaleString('en-US')
const formatMoney = value => value === undefined || value === null ? '—' : `$${toNumber(value).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
const summaryItems = computed(() => [
  { label: '有订单站点', value: formatNumber(total.value), note: '当前筛选全部结果', tone: 'blue' },
  { label: '去重订单', value: formatNumber(summary.value.deduplicated_orders), note: `原始订单 ${formatNumber(summary.value.total_orders)} 笔`, tone: 'violet' },
  { label: '成功订单', value: formatNumber(summary.value.successful_orders), note: '已支付去重订单', tone: 'green' },
  { label: '总订单金额', value: formatMoney(summary.value.total_amount), note: '当前筛选全部结果', tone: 'amber' },
  { label: '成功金额', value: formatMoney(summary.value.successful_amount), note: '已支付金额', tone: 'green' },
])

async function fetchData() {
  loading.value = true
  try {
    const res = await getSiteOrderRanking({
      page: page.value,
      size: size.value,
      userGroup: filters.userGroup || undefined,
      startDate: filters.dateRange?.[0] || undefined,
      endDate: filters.dateRange?.[1] || undefined,
      domain: filters.domain.trim() || undefined,
      adminName: filters.adminName.trim() || undefined,
      themeName: filters.themeName.trim() || undefined,
      minOrders: filters.minOrders ?? undefined,
      minSuccessfulOrders: filters.minSuccessfulOrders ?? undefined,
      minTotalAmount: filters.minTotalAmount ?? undefined,
      minSuccessfulAmount: filters.minSuccessfulAmount ?? undefined,
      minIndexCount: filters.minIndexCount ?? undefined,
      minProductCount: filters.minProductCount ?? undefined,
      sortBy: sortBy.value,
      sortDir: sortDir.value,
    })
    tableData.value = res.data.list || []
    total.value = res.data.total || 0
    summary.value = res.data.summary || {}
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchData()
}

function handleSizeChange(value) {
  size.value = value
  page.value = 1
  fetchData()
}

function handleSortChange({ prop, order }) {
  sortBy.value = order ? prop : 'deduplicated_orders'
  sortDir.value = order === 'ascending' ? 'asc' : 'desc'
  page.value = 1
  fetchData()
}

function resetFilters() {
  Object.assign(filters, defaultFilters())
  sortBy.value = 'deduplicated_orders'
  sortDir.value = 'desc'
  page.value = 1
  fetchData()
}

onMounted(async () => {
  await loadSiteGroups()
  await fetchData()
})
</script>

<style scoped>
.ranking-filter { margin-bottom: 16px; }
.ranking-filter :deep(.el-input), .ranking-filter :deep(.el-select) { width: 165px; }
.ranking-filter .date-filter :deep(.el-date-editor) { width: 260px; }
.ranking-filter .mini-input { width: 118px; }
.ranking-summary { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 11px; margin: 16px 0; }
.summary-item { position: relative; overflow: hidden; padding: 15px 16px; border: 1px solid var(--cf-line-soft); border-radius: 12px; background: #fafbfc; }
.summary-item::after { position: absolute; top: -20px; right: -18px; width: 62px; height: 62px; border-radius: 50%; background: currentColor; opacity: .06; content: ''; }
.summary-item span, .summary-item small { display: block; }
.summary-item span { color: var(--cf-muted); font-size: 11px; }
.summary-item strong { display: block; margin: 8px 0 5px; color: var(--cf-ink); font-size: 20px; letter-spacing: -.035em; }
.summary-item small { color: var(--cf-subtle); font-size: 9px; }
.summary-item.blue { color: var(--cf-blue); }
.summary-item.green { color: var(--cf-green); }
.summary-item.violet { color: var(--cf-violet); }
.summary-item.amber { color: #d79a36; }
@media (max-width: 1100px) { .ranking-summary { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 720px) {
  .ranking-summary { grid-template-columns: repeat(2, 1fr); }
  .ranking-filter :deep(.el-input), .ranking-filter :deep(.el-select), .ranking-filter .date-filter :deep(.el-date-editor) { width: 100%; }
}
</style>
