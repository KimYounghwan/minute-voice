"""Smoke test on an emulator with the instrumentation test's fixture installed."""
import subprocess, time, re, xml.etree.ElementTree as ET
ADB='/Users/yhkim/Library/Android/sdk/platform-tools/adb'
def adb(*args): return subprocess.check_output([ADB,'-e',*args],text=True)
def nodes():
    adb('shell','uiautomator','dump','/sdcard/minutevoice-ui.xml')
    return list(ET.fromstring(adb('shell','cat','/sdcard/minutevoice-ui.xml')).iter('node'))
def tap(node):
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.attrib['bounds']))
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
def find(text):
    for _ in range(9):
        for node in nodes():
            if node.attrib.get('text')==text:return node
        adb('shell','input','swipe','985','2000','985','650','350')
    raise AssertionError(text+' not found')
adb('shell','am','force-stop','kr.co.minutevoice')
adb('shell','am','start','-n','kr.co.minutevoice/.MainActivity')
tap(find('＋ 알람 추가'))
tap(find('알람 저장하고 켜기'))
ns=nodes(); switches=[n for n in ns if n.attrib['class']=='android.widget.Switch']
assert len(switches)==2, 'two alarms shown'
assert len([n for n in ns if n.attrib.get('text')=='매일'])==2
old=[n.attrib['checked'] for n in switches];tap(switches[0])
ns=nodes();new=[n.attrib['checked'] for n in ns if n.attrib['class']=='android.widget.Switch']
assert new[0]!=old[0] and new[1]==old[1], 'independent activation'
tap(next(n for n in ns if n.attrib.get('text')=='오전 7:00'))
find('변경한 설정 저장' if new[0]=='true' else '알람 저장하고 켜기')
adb('shell','input','keyevent','4')
ns=nodes();assert len([n for n in ns if n.attrib['class']=='android.widget.Switch'])==2
print('UI passed: add, list times/daily labels, independent toggle, edit, return')
