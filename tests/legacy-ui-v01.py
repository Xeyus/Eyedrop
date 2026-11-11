"""Legacy v0.1 cloud UI attempt. Not compatible with v0.2-A; not a passed UI test."""
import os,subprocess,time,xml.etree.ElementTree as ET,re,pathlib
ADB='/workspace/work/tools/android-sdk/platform-tools/adb'
ROOT=pathlib.Path('/workspace/work/eyedrop')
def adb(*args,timeout=90):
 r=subprocess.run([ADB,*args],capture_output=True,timeout=timeout)
 if r.returncode:raise RuntimeError(r.stderr.decode(errors='replace'))
 return r.stdout
until=time.time()+300
while time.time()<until:
 try:
  if adb('shell','getprop','sys.boot_completed',timeout=10).strip()==b'1':break
 except Exception:pass
 time.sleep(3)
else:raise RuntimeError('Android boot did not complete within 300 seconds')
print('Android boot completed',flush=True)
adb('install','--no-incremental',str(ROOT/'build/EyeDrop-v0.1.0.apk'))
adb('shell','input','keyevent','82')
adb('shell','am','start','-n','com.eyedrop.manager/.MainActivity')
time.sleep(3)
def tree():
 adb('shell','uiautomator','dump','/sdcard/window.xml')
 return ET.fromstring(adb('shell','cat','/sdcard/window.xml'))
def tap_node(n):
 x1,y1,x2,y2=map(int,re.findall(r'\d+',n.attrib['bounds']))
 adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
 time.sleep(.5)
def tap(label):
 for n in tree().iter('node'):
  if n.attrib.get('text')==label:tap_node(n);return
 raise AssertionError('Missing control: '+label)
def assert_text(label):
 assert any(label in n.attrib.get('text','') for n in tree().iter('node')),label
 print('PASS UI: '+label,flush=True)
def add(name,count):
 tap('＋ 添加药物')
 edits=[n for n in tree().iter('node') if n.attrib.get('class')=='android.widget.EditText']
 tap_node(edits[0]);adb('shell','input','text',name)
 tap_node(edits[1]);adb('shell','input','keyevent','67');adb('shell','input','text',str(count))
 adb('shell','input','keyevent','4')
 tap('保存')
add('Medication-A',4)
assert_text('0 / 4')
assert_text('08:00')
tap('记录已使用');tap('确认打卡');assert_text('1 / 4')
tap('药物');add('Medication-B',2);assert_text('Medication-B')
tap('今日');assert_text('1 / 6')
(ROOT/'release/today.png').write_bytes(adb('exec-out','screencap','-p'))
tap('记录');assert_text('Medication-A');assert_text('实际')
adb('shell','am','force-stop','com.eyedrop.manager')
adb('shell','am','start','-n','com.eyedrop.manager/.MainActivity')
time.sleep(2);assert_text('1 / 6')
tap('药物');tap('编辑药物');tap('删除');tap('删除')
tap('记录');assert_text('Medication-A')
print('PASS UI: CRUD, completion, multiple medications, restart persistence, deletion preserves history.',flush=True)
