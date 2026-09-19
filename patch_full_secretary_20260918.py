from pathlib import Path
import re
p=Path(r"C:\Users\graph\Documents\directeur portaille\secretary.html")
s=p.read_text(encoding="utf-8",errors="replace")
backup=p.with_name("secretary_backup_before_parallel_pdf_trash_20260918.html")
backup.write_text(s,encoding="utf-8")

# Correct the security message.
s=s.replace("cette opération ne supprime aucun Ã©lÃ¨ve, paiement, bulletin ou document. Elle change seulement lâ€™annÃ©e active dâ€™affichage.",
            "cette opération ne supprime aucun élève, paiement, bulletin ou document. Elle change seulement l’année active d’affichage.")

# Replace enrolled export with PDF export.
a=s.index("      function exportSecretaryEnrolledStudents(){")
b=s.index("      function onEnrollmentBranchChange",a)
pdf_fn="""      async function exportSecretaryEnrolledStudents(){
        const table=document.getElementById('enrolledStudentsTable');
        if(!table){showNotification('Liste des élèves inscrits introuvable.','warning');return;}
        const trs=[...table.querySelectorAll('tr')].filter(tr=>tr.querySelector('td'));
        if(!trs.length || (trs.length===1 && trs[0].innerText.includes('Aucune inscription'))){showNotification('Aucun élève inscrit à exporter.','warning');return;}
        const year=getActiveSecretarySchoolYear();
        const branch=document.getElementById('enrolledBranchFilter')?.value||'';
        const root=document.createElement('div');
        root.style.cssText='background:#fff;color:#111;padding:24px;width:794px;font-family:Arial,sans-serif;';
        const title=document.createElement('h2');
        title.textContent='EDEN FAMILY SCHOOL — LISTE DES ÉLÈVES INSCRITS';
        title.style.cssText='text-align:center;margin:0 0 6px;font-size:20px;';
        const sub=document.createElement('div');
        sub.textContent='Année scolaire : '+year+' | Branche : '+(branch?secretaryBranchLabel(branch):'Toutes les branches');
        sub.style.cssText='text-align:center;margin-bottom:18px;font-size:12px;';
        root.appendChild(title);root.appendChild(sub);
        const out=document.createElement('table');
        out.style.cssText='width:100%;border-collapse:collapse;font-size:10px;';
        const head=document.createElement('tr');
        ['N°','Nom de l’élève','Branche','Classe','Parent','Année','Statut'].forEach(h=>{
          const th=document.createElement('th');th.textContent=h;th.style.cssText='border:1px solid #555;padding:7px;background:#e9eef5;text-align:left;';head.appendChild(th);
        });
        out.appendChild(head);
        trs.forEach((tr,i)=>{
          const td=[...tr.querySelectorAll('td')];
          const vals=[String(i+1),td[0]?.innerText||'',td[1]?.innerText||'',td[2]?.innerText||'',td[3]?.innerText||'',td[4]?.innerText||'',td[5]?.innerText||''];
          const row=document.createElement('tr');
          vals.forEach(v=>{const c=document.createElement('td');c.textContent=v.replace(/\s+/g,' ').trim();c.style.cssText='border:1px solid #777;padding:6px;vertical-align:top;';row.appendChild(c);});
          out.appendChild(row);
        });
        root.appendChild(out);
        const footer=document.createElement('div');footer.textContent='Total : '+trs.length+' élève(s)';footer.style.cssText='margin-top:12px;font-weight:700;font-size:11px;';root.appendChild(footer);
        document.body.appendChild(root);
        try{
          if(typeof html2pdf!=='function') throw new Error('Le module PDF n’est pas disponible.');
          await html2pdf().set({margin:0.25,filename:'Eleves_Inscrits_'+year+'.pdf',image:{type:'jpeg',quality:0.98},html2canvas:{scale:2,useCORS:true,backgroundColor:'#ffffff'},jsPDF:{unit:'in',format:'a4',orientation:'portrait'},pagebreak:{mode:['css','legacy']}}).from(root).save();
          showNotification(trs.length+' élève(s) exporté(s) en PDF pour '+year+'.','success');
        }catch(e){console.error('Export PDF élèves inscrits:',e);showNotification('Erreur export PDF : '+e.message,'danger');}
        finally{root.remove();}
      }

"""
s=s[:a]+pdf_fn+s[b:]

# Nursery progression: N1A/N1B -> N2; never A -> B.
pattern=r"      function delibSuggestNextClass\(branch, className\) \{.*?\n      \}\n      function delibIsPrimaryClass"
new="""      function delibSuggestNextClass(branch,className){
        const token=normalizeClassWithoutBranchSuffixForBulletin(className).toUpperCase();
        const m=token.match(/^N(\\d+)$/);
        if(m){
          const nextBase='N'+(Number(m[1])+1);
          const key=normalizeKey(branch);
          const candidates=(classesData||[]).filter(c=>{
            const cb=normalizeKey(c.branch||c.branche);
            const n=String(c.name||c.class||c.classe||'').trim();
            return (!key||cb===key)&&normalizeClassWithoutBranchSuffixForBulletin(n).toUpperCase()===nextBase;
          }).map(c=>String(c.name||c.class||c.classe||'').trim()).filter(Boolean);
          const exact=candidates.find(n=>normalizeKey(n)===normalizeKey(nextBase))||candidates[0];
          if(exact)return {branch:branch,class:exact,crossBranch:false,endOfCycle:false,nurseryProgression:true};
          if(Number(m[1])>=3)return {branch:'',class:'',crossBranch:false,endOfCycle:true};
        }
        const seq=delibBuildSequence(branch);
        const idx=seq.findIndex(c=>normalizeKey(c)===normalizeKey(className));
        if(idx>=0&&idx<seq.length-1)return {branch:branch,class:seq[idx+1],crossBranch:false,endOfCycle:false};
        if(token==='N3'&&normalizeKey(branch)!=='gisozi'){
          const gisoziSeq=delibBuildSequence('gisozi').filter(c=>/^P1/i.test(normalizeClassWithoutBranchSuffixForBulletin(c)));
          return {branch:'gisozi',class:gisoziSeq[0]||'',crossBranch:true,endOfCycle:false};
        }
        if(token==='P5')return {branch:'',class:'',crossBranch:false,endOfCycle:true};
        return {branch:branch,class:'',crossBranch:false,endOfCycle:false,unknown:true};
      }
      function delibIsPrimaryClass"""
s,n=re.subn(pattern,lambda m:new,s,flags=re.S)
print("suggest replacement",n)

# Skip sibling split modal for nursery parallel classes.
old="""        if (!skipTransitionCheck && !delibState.transitionConfirmed[className]) {
          const planStudents=Object.keys(bucket).map(sid=>studentsData.find(st=>st.id===sid)).filter(Boolean);
          const parallel=delibParallelClasses(branch,className);
          if(parallel.length>=2 && planStudents.length){
            const plan=delibBuildBalancedSplitPlan(planStudents,parallel);
            delibState.transitionPlans[className]=plan;
            delibShowTransitionModal(className,planStudents,parallel,plan);
            return;
          }
        }"""
new="""        const nurseryParallel=/^N\\d+[A-Z]$/i.test(String(className||'').trim());
        if (!skipTransitionCheck && !delibState.transitionConfirmed[className] && !nurseryParallel) {
          const planStudents=Object.keys(bucket).map(sid=>studentsData.find(st=>st.id===sid)).filter(Boolean);
          const parallel=delibParallelClasses(branch,className);
          if(parallel.length>=2 && planStudents.length){
            const plan=delibBuildBalancedSplitPlan(planStudents,parallel);
            delibState.transitionPlans[className]=plan;
            delibShowTransitionModal(className,planStudents,parallel,plan);
            return;
          }
        }"""
s=s.replace(old,new,1)

# Add Corbeille tab to Paramètres Avancés.
nav='''              <li class="nav-item" role="presentation">
                <button class="nav-link d-flex align-items-center justify-content-center" id="language-tab" data-bs-toggle="pill" data-bs-target="#language" type="button" role="tab">'''
nav_new='''              <li class="nav-item" role="presentation">
                <button class="nav-link d-flex align-items-center justify-content-center" id="secretary-trash-tab" data-bs-toggle="pill" data-bs-target="#secretary-trash" type="button" role="tab" onclick="loadSecretaryTrash()">
                  <i class="bi bi-trash3 me-2"></i>Corbeille
                </button>
              </li>
'''+nav
s=s.replace(nav,nav_new,1)

pane='''              <!-- Onglet Langue -->'''
trash_pane='''              <!-- Onglet Corbeille -->
              <div class="tab-pane fade" id="secretary-trash" role="tabpanel">
                <div class="card border-0 shadow-sm">
                  <div class="card-body">
                    <div class="d-flex justify-content-between align-items-center mb-4">
                      <div>
                        <h5 class="card-title mb-1"><i class="bi bi-trash3 text-danger me-2"></i>Corbeille</h5>
                        <small class="text-muted">Les éléments supprimés par le secrétariat restent restaurables jusqu’à leur suppression définitive.</small>
                      </div>
                      <div class="d-flex gap-2">
                        <button class="btn btn-outline-primary btn-sm" onclick="loadSecretaryTrash()"><i class="bi bi-arrow-clockwise me-1"></i>Actualiser</button>
                        <button class="btn btn-outline-danger btn-sm" onclick="emptySecretaryTrash()"><i class="bi bi-trash3-fill me-1"></i>Vider la corbeille</button>
                      </div>
                    </div>
                    <div class="alert alert-warning">
                      <i class="bi bi-shield-exclamation me-2"></i>Restaurer remet les données à leur emplacement d’origine. La suppression définitive les retire de Firebase de façon irréversible.
                    </div>
                    <div class="table-responsive">
                      <table class="table table-hover align-middle">
                        <thead><tr><th>Élément</th><th>Source</th><th>Date</th><th>Motif</th><th>Actions</th></tr></thead>
                        <tbody id="secretaryTrashTableBody"><tr><td colspan="5" class="text-center text-muted py-4">Ouvrez la Corbeille pour charger les éléments.</td></tr></tbody>
                      </table>
                    </div>
                  </div>
                </div>
              </div>

'''+pane
s=s.replace(pane,trash_pane,1)

# Generic Secretary trash functions.
trash_js="""      // ===== CORBEILLE SECRÉTAIRE =====
      const secretaryTrashRef = database.ref('trash');
      async function moveToSecretaryTrash(collection,recordId,options={}){
        if(!collection||!recordId)throw new Error('Identifiant manquant.');
        const ref=database.ref(String(collection).replace(/^\\/+|\\/+$/g,''));
        const snap=await ref.child(recordId).once('value');
        if(!snap.exists())return false;
        const v=snap.val()||{};
        const item={
          originalPath:String(collection).replace(/^\\/+|\\/+$/g,'')+'/'+recordId,
          originalId:String(recordId),
          originalData:v,
          deletedAt:Date.now(),
          deletedBy:currentUser?.email||'Secrétariat',
          deletedByUid:currentUser?.uid||'',
          reason:options.reason||'Suppression depuis Secretary',
          source:options.source||'secretary',
          label:options.label||v.name||v.studentName||v.title||String(recordId)
        };
        await secretaryTrashRef.push(item);
        await ref.child(recordId).remove();
        return true;
      }
      async function restoreSecretaryTrash(trashId){
        const snap=await secretaryTrashRef.child(trashId).once('value');
        if(!snap.exists())throw new Error('Élément introuvable dans la Corbeille.');
        const item=snap.val()||{},parts=String(item.originalPath||'').split('/').filter(Boolean);
        if(parts.length<2)throw new Error('Emplacement d’origine invalide.');
        let ref=database.ref(parts[0]);
        for(let i=1;i<parts.length-1;i++)ref=ref.child(parts[i]);
        await ref.child(parts[parts.length-1]).set(item.originalData);
        await secretaryTrashRef.child(trashId).remove();
      }
      async function permanentlyDeleteSecretaryTrash(trashId){
        if(!trashId)return;
        if(!confirm('Supprimer définitivement cet élément ? Cette action est irréversible.'))return;
        await secretaryTrashRef.child(trashId).remove();
        loadSecretaryTrash();
      }
      async function emptySecretaryTrash(){
        const snap=await secretaryTrashRef.once('value');
        if(!snap.exists()){showNotification('La Corbeille est déjà vide.','info');return;}
        if(!confirm('Vider définitivement toute la Corbeille ? Cette action est irréversible.'))return;
        await secretaryTrashRef.remove();
        loadSecretaryTrash();
        showNotification('Corbeille vidée.','success');
      }
      async function loadSecretaryTrash(){
        const body=document.getElementById('secretaryTrashTableBody');if(!body)return;
        body.innerHTML='<tr><td colspan="5" class="text-center py-4"><i class="bi bi-arrow-repeat spin me-2"></i>Chargement...</td></tr>';
        try{
          const snap=await secretaryTrashRef.once('value'),rows=[];
          snap.forEach(ch=>{const x=ch.val()||{};rows.push({id:ch.key,...x});});
          rows.sort((a,b)=>(b.deletedAt||0)-(a.deletedAt||0));
          if(!rows.length){body.innerHTML='<tr><td colspan="5" class="text-center text-muted py-5"><i class="bi bi-trash3 fs-2 d-block mb-2"></i>La Corbeille est vide.</td></tr>';return;}
          body.innerHTML=rows.map(x=>{
            const label=String(x.label||x.originalId).replace(/[&<>"']/g,function(ch){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',\"'\":'&#39;'}[ch];});
            const path=String(x.originalPath||'');
            const source=String(x.source||'system');
            const reason=String(x.reason||'-');
            const date=x.deletedAt?new Date(x.deletedAt).toLocaleString('fr-FR'):'-';
            return '<tr><td><strong>'+label+'</strong><br><small class="text-muted">'+path+'</small></td><td>'+source+'</td><td>'+date+'</td><td>'+reason+'</td><td class="text-nowrap"><button class="btn btn-sm btn-outline-success me-1" onclick="restoreSecretaryTrash(\\''+x.id+'\\').then(loadSecretaryTrash)"><i class="bi bi-arrow-counterclockwise"></i></button><button class="btn btn-sm btn-outline-danger" onclick="permanentlyDeleteSecretaryTrash(\\''+x.id+'\\')"><i class="bi bi-x-circle"></i></button></td></tr>';
          }).join('');
        }catch(e){console.error(e);body.innerHTML='<tr><td colspan="5" class="text-center text-danger py-4">Impossible de charger la Corbeille : '+(e.message||e)+'</td></tr>';}
      }

"""
s=s.replace('      function deleteUserPrompt(id) {',trash_js+'      function deleteUserPrompt(id) {',1)

# Convert existing direct destructive functions to trash.
old="""      async function deleteUser(id) {
        try {
          await database.ref('users/' + id).remove();
          alert('Utilisateur supprimé avec succès!');
          loadUsers();
        } catch (error) {
          console.error('Erreur suppression utilisateur:', error);
          alert('Erreur: ' + error.message);
        }
      }"""
new="""      async function deleteUser(id) {
        try {
          const u=usersData.find(x=>x.id===id)||{};
          await moveToSecretaryTrash('users',id,{source:'users',reason:'Suppression d’un utilisateur',label:u.name||u.email||id});
          showNotification('Utilisateur placé dans la Corbeille.','success');
          loadUsers();
        } catch (error) {
          console.error('Erreur suppression utilisateur:', error);
          alert('Erreur: ' + error.message);
        }
      }"""
s=s.replace(old,new,1)

s=s.replace("        database.ref('classes/' + id).remove();",
            "        moveToSecretaryTrash('classes',id,{source:'classes',reason:'Suppression d’une classe'}).then(()=>{showNotification('Classe placée dans la Corbeille.','success');loadClasses();}).catch(e=>showNotification('Erreur : '+e.message,'danger'));",1)

old="""      function deleteStudent(id) {
        if (confirm('Supprimer cet élève ?')) {
          database.ref('students/' + id).remove();
        }
      }"""
new="""      function deleteStudent(id) {
        if (!confirm('Supprimer cet élève ? Il sera placé dans la Corbeille.')) return;
        const st=studentsData.find(x=>x.id===id)||{};
        moveToSecretaryTrash('students',id,{source:'students',reason:'Suppression d’un élève',label:st.name||id})
          .then(()=>{showNotification('Élève placé dans la Corbeille.','success');loadStudents(currentUser?.uid);})
          .catch(e=>showNotification('Erreur : '+e.message,'danger'));
      }"""
s=s.replace(old,new,1)

s=s.replace("            await database.ref(entry._firebasePath).remove();",
            "            const pathParts=String(entry._firebasePath||'').split('/').filter(Boolean); const rid=pathParts.pop(); const coll=pathParts.join('/'); await moveToSecretaryTrash(coll,rid,{source:'bulletin-duplicates',reason:'Bulletin en double détecté',label:entry.studentName||entry._firebasePath});",1)

p.write_text(s,encoding="utf-8")
print("PATCH COMPLETE",len(s),"backup",backup)
