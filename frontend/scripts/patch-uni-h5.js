const fs = require('fs')
const path = require('path')

function patchFile(filePath, patches) {
  if (!fs.existsSync(filePath)) {
    console.warn(`[patch-uni-h5] skip missing file: ${filePath}`)
    return
  }

  let source = fs.readFileSync(filePath, 'utf8')
  let changed = false

  for (const [from, to] of patches) {
    if (source.includes(from)) {
      source = source.replace(from, to)
      changed = true
    }
  }

  if (changed) {
    fs.writeFileSync(filePath, source)
    console.log(`[patch-uni-h5] patched ${path.relative(process.cwd(), filePath)}`)
  }
}

const uniPluginRoot = path.join(__dirname, '..', 'node_modules', '@dcloudio', 'vue-cli-plugin-uni')

patchFile(path.join(uniPluginRoot, 'lib', 'vue-loader.js'), [
  [
    '.tap(options => Object.assign(options, vueLoader.options(loaderOptions, compilerOptions), cacheConfig))',
    '.tap(options => Object.assign(options || {}, vueLoader.options(loaderOptions, compilerOptions), cacheConfig))'
  ],
  [
    '.tap(options => Object.assign(options, api.genCacheConfig(',
    '.tap(options => Object.assign(options || {}, api.genCacheConfig('
  ]
])

patchFile(path.join(uniPluginRoot, 'packages', 'vue-loader', 'lib', 'loaders', 'templateLoader.js'), [
  [
    'return code + `\\nexport { render, staticRenderFns, recyclableRender, components }`',
    "return code + `\\nvar recyclableRender = typeof recyclableRender === 'undefined' ? undefined : recyclableRender\\nvar components = typeof components === 'undefined' ? undefined : components\\nexport { render, staticRenderFns, recyclableRender, components }`"
  ]
])
