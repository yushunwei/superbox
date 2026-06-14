<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'

onLaunch(() => {
  console.log('Superbox MiniApp launched')
})
</script>

<style lang="scss">
page {
  background-color: #0d1117;
  color: #e6edf3;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 14px;
}

view, text, button, input, textarea, navigator {
  box-sizing: border-box;
}

/* Scrollbar hide for mini program */
::-webkit-scrollbar {
  display: none;
  width: 0;
  height: 0;
}
</style>
