// 로컬에서 띄운 백엔드 서비스의 /v3/api-docs를 받아 openapi/<서비스>.json에 저장한다.
//   pnpm api:pull            # 모든 서비스
//   pnpm api:pull product    # 하나만
// 받은 뒤 pnpm api:generate로 코드를 다시 만든다. 응답을 못 받은 서비스는 건너뛰고 기존 파일을 둔다.
import { readFileSync, writeFileSync } from 'node:fs'

const services = JSON.parse(readFileSync('openapi/services.json', 'utf8'))
const only = process.argv.slice(2)

for (const [service, port] of Object.entries(services)) {
  if (only.length > 0 && !only.includes(service)) continue
  const url = `http://localhost:${port}/v3/api-docs`
  try {
    const response = await fetch(url)
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    const spec = await response.json()
    // 받은 주소(servers)는 PC마다 달라 diff만 만든다
    delete spec.servers
    writeFileSync(`openapi/${service}.json`, `${JSON.stringify(spec, null, 2)}\n`)
    console.log(`ok   ${service} ← ${url}`)
  } catch (error) {
    console.warn(`skip ${service} (${url}): ${error.message}`)
  }
}
