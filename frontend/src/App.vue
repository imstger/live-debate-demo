<script>
	export default {
		onLaunch: function() {
			console.log('App Launch')
		},
		onShow: function() {
			console.log('App Show')
		},
		onHide: function() {
			console.log('App Hide')
		}
	}
</script>

<style>
	/*每个页面公共css */
	@import "~vant-weapp/dist/common/index.wxss";
	/* 全局引入IconFont */
	@import "static/iconfont/iconfont.css";
	/* 引入uni-icons CSS */
	@import "~@dcloudio/uni-ui/lib/uni-icons/uniicons.css";
</style>
