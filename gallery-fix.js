(function(){
  let items=[], current=0, timer;
  function render(){
    const track=document.getElementById('galleryCarouselTrack'),dots=document.getElementById('galleryCarouselDots');
    if(!track||!dots)return;
    if(!items.length){track.innerHTML='<div class="gallery-empty">Aucune image disponible pour le moment.</div>';dots.innerHTML='';return;}
    track.innerHTML=items.map((x,i)=>`<div class="gallery-slide ${i===current?'is-active':''}" data-index="${i}"><img src="${x.url||x.src}" alt="${x.name||'Eden Family School'}" loading="${i?'lazy':'eager'}"></div>`).join('');
    dots.innerHTML=items.map((_,i)=>`<button class="gallery-dot ${i===current?'active':''}" onclick="galleryCarouselGo(${i})" aria-label="Image ${i+1}"></button>`).join('');
  }
  function go(n){if(!items.length)return;current=(n+items.length)%items.length;render();restart();}
  function restart(){clearInterval(timer);if(items.length>1)timer=setInterval(()=>go(current+1),5000);}
  window.galleryCarouselGo=go;
  window.galleryCarouselPrev=()=>go(current-1);
  window.galleryCarouselNext=()=>go(current+1);
  function init(){
    if(typeof firebase==='undefined'||typeof database==='undefined')return;
    database.ref('carousel').once('value').then(s=>{
      const data=s.val(); items=Array.isArray(data?.items)?data.items:Object.values(data?.items||{});
      items=items.filter(x=>x&&(x.url||x.src)&&x.type!=='video');render();restart();
    }).catch(e=>console.warn('Galerie carousel:',e));
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();
