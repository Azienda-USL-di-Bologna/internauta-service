package it.bologna.ausl.internauta.service.krint;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.service.repositories.scripta.ArchivioRepository;
import it.bologna.ausl.internauta.service.utils.CachedEntities;
import it.bologna.ausl.internauta.utils.bds.types.EntitaStoredProcedure;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.logs.Krint;
import static it.bologna.ausl.model.entities.logs.Krint.TipoOggettoKrint.SCRIPTA_DOC;
import it.bologna.ausl.model.entities.logs.OperazioneKrint;
import it.bologna.ausl.model.entities.logs.projections.KrintBaborgPersona;
import it.bologna.ausl.model.entities.logs.projections.KrintBaborgStruttura;
import it.bologna.ausl.model.entities.logs.projections.KrintScriptaArchivio;
import it.bologna.ausl.model.entities.logs.projections.KrintScriptaAttoreArchivio;
import it.bologna.ausl.model.entities.logs.projections.KrintScriptaDoc;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.ArchivioDoc;
import it.bologna.ausl.model.entities.scripta.AttoreArchivio;
import it.bologna.ausl.model.entities.scripta.Doc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.stereotype.Service;

/**
 *
 * @author gusgus
 */
@Service
public class KrintScriptaService {

    @Autowired
    private ProjectionFactory factory;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KrintService krintService;

    @Autowired
    private CachedEntities cachedEntities;

    @Autowired
    private ArchivioRepository archivioRepository;

    public void writeArchivioCreation(Archivio archivio, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintScriptaArchivio krintScriptaArchivio = factory.createProjection(KrintScriptaArchivio.class, archivio);
            String jsonKrintArchivio = objectMapper.writeValueAsString(krintScriptaArchivio);

            krintService.writeKrintRow(
                    archivio.getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO, // tipoOggetto
                    archivio.getNumerazioneGerarchica(), // descrizioneOggetto
                    jsonKrintArchivio, // informazioniOggetto
                    null, // Da qui si ripete ma per il conenitore
                    null,
                    null,
                    null,
                    codiceOperazione);
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = archivio.getId();
            } catch (Exception exa) {
            }
            krintService.writeKrintError(idOggetto, "writeArchivioCreation", codiceOperazione);
        }
    }

    public void writeArchivioUpdate(Archivio archivio, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintScriptaArchivio krintScriptaArchivio = factory.createProjection(KrintScriptaArchivio.class, archivio);
            String jsonKrintArchivio = objectMapper.writeValueAsString(krintScriptaArchivio);

            krintService.writeKrintRow(
                    archivio.getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO, // tipoOggetto
                    archivio.getNumerazioneGerarchica(), // descrizioneOggetto
                    jsonKrintArchivio, // informazioniOggetto
                    null, // Da qui si ripete ma per il conenitore
                    null,
                    null,
                    null,
                    codiceOperazione);
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = archivio.getId();
            } catch (Exception exa) {
            }
            krintService.writeKrintError(idOggetto, "writeArchivioUpdate", codiceOperazione);
        }
    }

    /**
     * @param attoreArchivio
     * @param codiceOperazione
     */
    public void writeAttoreArchivioCreation(AttoreArchivio attoreArchivio, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintScriptaAttoreArchivio krintScriptaAttoreArchivio = factory.createProjection(KrintScriptaAttoreArchivio.class, attoreArchivio);
            String jsonKrintAttore = objectMapper.writeValueAsString(krintScriptaAttoreArchivio);

            // Informazioni oggetto contenitore
            KrintScriptaArchivio krintScriptaArchivio = factory.createProjection(KrintScriptaArchivio.class, attoreArchivio.getIdArchivio());
            String jsonKrintArchivio = objectMapper.writeValueAsString(krintScriptaArchivio);

            krintService.writeKrintRow(
                    attoreArchivio.getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ATTORE_ARCHIVIO, // tipoOggetto
                    attoreArchivio.getIdPersona().getDescrizione(), // descrizioneOggetto
                    jsonKrintAttore, // informazioniOggetto
                    attoreArchivio.getIdArchivio().getId().toString(), // Da qui si ripete ma per il conenitore
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO,
                    attoreArchivio.getIdArchivio().getNumerazioneGerarchica(),
                    jsonKrintArchivio,
                    codiceOperazione);
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = attoreArchivio.getId();
            } catch (Exception exa) {
            }
            krintService.writeKrintError(idOggetto, "writeAttoreArchivioCreation", codiceOperazione);
        }
    }

    /**
     * @param attoreArchivio
     * @param codiceOperazione
     */
    public void writeAttoreArchivioUpdate(AttoreArchivio attoreArchivio, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintScriptaAttoreArchivio krintScriptaAttoreArchivio = factory.createProjection(KrintScriptaAttoreArchivio.class, attoreArchivio);
            String jsonKrintAttore = objectMapper.writeValueAsString(krintScriptaAttoreArchivio);

            // Informazioni oggetto contenitore
            KrintScriptaArchivio krintScriptaArchivio = factory.createProjection(KrintScriptaArchivio.class, attoreArchivio.getIdArchivio());
            String jsonKrintArchivio = objectMapper.writeValueAsString(krintScriptaArchivio);

            krintService.writeKrintRow(
                    attoreArchivio.getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ATTORE_ARCHIVIO, // tipoOggetto
                    attoreArchivio.getIdPersona().getDescrizione(), // descrizioneOggetto
                    jsonKrintAttore, // informazioniOggetto
                    attoreArchivio.getIdArchivio().getId().toString(), // Da qui si ripete ma per il conenitore
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO,
                    attoreArchivio.getIdArchivio().getNumerazioneGerarchica(),
                    jsonKrintArchivio,
                    codiceOperazione);
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = attoreArchivio.getId();
            } catch (Exception exa) {
            }
            krintService.writeKrintError(idOggetto, "writeAttoreArchivioUpdate", codiceOperazione);
        }
    }

    /**
     * @param attoreArchivio
     * @param codiceOperazione
     */
    public void writeAttoreArchivioDelete(AttoreArchivio attoreArchivio, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintScriptaAttoreArchivio krintScriptaAttoreArchivio = factory.createProjection(KrintScriptaAttoreArchivio.class, attoreArchivio);
            String jsonKrintAttore = objectMapper.writeValueAsString(krintScriptaAttoreArchivio);

            // Informazioni oggetto contenitore
            KrintScriptaArchivio krintScriptaArchivio = factory.createProjection(KrintScriptaArchivio.class, attoreArchivio.getIdArchivio());
            String jsonKrintArchivio = objectMapper.writeValueAsString(krintScriptaArchivio);

            krintService.writeKrintRow(
                    attoreArchivio.getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ATTORE_ARCHIVIO, // tipoOggetto
                    attoreArchivio.getIdPersona().getDescrizione(), // descrizioneOggetto
                    jsonKrintAttore, // informazioniOggetto
                    attoreArchivio.getIdArchivio().getId().toString(), // Da qui si ripete ma per il conenitore
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO,
                    attoreArchivio.getIdArchivio().getNumerazioneGerarchica(),
                    jsonKrintArchivio,
                    codiceOperazione);
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = attoreArchivio.getId();
            } catch (Exception exa) {
            }
            krintService.writeKrintError(idOggetto, "writeAttoreArchivioDelete", codiceOperazione);
        }
    }

    /**
     *
     * @param struttura l'ufficio soggetto
     * @param codiceOperazione il tipo di operazione
     * @param oggetto l'oggetto dell'operazione
     */
    public void writePermessiArchivio(Integer idArchivio, EntitaStoredProcedure entita, String predicato, OperazioneKrint.CodiceOperazione codiceOperazione) {
        Archivio archivio = archivioRepository.getById(Integer.SIZE);
        try {

            KrintScriptaArchivio krintScriptaArchivio = factory.createProjection(KrintScriptaArchivio.class, archivio);
            String jsonKrintArchivio = objectMapper.writeValueAsString(krintScriptaArchivio);
            jsonKrintArchivio = jsonKrintArchivio.replace("}", ", \"predicato\": \"" + predicato + "\"}");

            String idOggetto = null;
            String descrizioneOggetto = null;
            Krint.TipoOggettoKrint tipoOggetto = null;
            String jsonKrintEntitaPermessoArchivio = null;
            switch (entita.getTable()) {
                case "persone":
                    Persona persona = cachedEntities.getPersona(entita.getIdProvenienza());
                    KrintBaborgPersona krintBaborgPersona = factory.createProjection(KrintBaborgPersona.class, persona);
                    jsonKrintEntitaPermessoArchivio = objectMapper.writeValueAsString(krintBaborgPersona);
                    idOggetto = krintBaborgPersona.getId().toString();
                    descrizioneOggetto = krintBaborgPersona.getDescrizione();
                    tipoOggetto = Krint.TipoOggettoKrint.BABORG_PERSONA;
                    break;
                case "strutture":
                    Struttura struttura = cachedEntities.getStruttura(entita.getIdProvenienza());
                    KrintBaborgStruttura krintBaborgStruttura = factory.createProjection(KrintBaborgStruttura.class, struttura);
                    jsonKrintEntitaPermessoArchivio = objectMapper.writeValueAsString(krintBaborgStruttura);
                    idOggetto = krintBaborgStruttura.getId().toString();
                    descrizioneOggetto = krintBaborgStruttura.getNome();
                    tipoOggetto = Krint.TipoOggettoKrint.BABORG_STRUTTURA;
                    break;
            }
//
//            switch(codiceOperazione){
//                case SCRIPTA_ARCHIVIO_PERMESSI_CREATION:
//                    AttributiStruttura attributiStruttura = (AttributiStruttura) oggetto;
//                    TipologiaStruttura tipologiaStruttura = attributiStruttura.getIdTipologiaStruttura();
//                    idOggetto = tipologiaStruttura.getId().toString();
//                    descrizioneOggetto = tipologiaStruttura.getTipologia();
//                    tipoOggetto = Krint.TipoOggettoKrint.BABORG_TIPOLOGIA_STRUTTURA;
//                    KrintBaborgTipologiaStruttura krintBaborgTipologiaStruttura = factory.createProjection(KrintBaborgTipologiaStruttura.class, tipologiaStruttura);
//                    jsonKrintOggetto = objectMapper.writeValueAsString(krintBaborgTipologiaStruttura);
//                    break;
//                case SCRIPTA_ARCHIVIO_PERMESSI_UPDATE:
//                    AttributiStruttura attributiStruttura = (AttributiStruttura) oggetto;
//                    TipologiaStruttura tipologiaStruttura = attributiStruttura.getIdTipologiaStruttura();
//                    idOggetto = tipologiaStruttura.getId().toString();
//                    descrizioneOggetto = tipologiaStruttura.getTipologia();
//                    tipoOggetto = Krint.TipoOggettoKrint.BABORG_TIPOLOGIA_STRUTTURA;
//                    KrintBaborgTipologiaStruttura krintBaborgTipologiaStruttura = factory.createProjection(KrintBaborgTipologiaStruttura.class, tipologiaStruttura);
//                    jsonKrintOggetto = objectMapper.writeValueAsString(krintBaborgTipologiaStruttura);
//                    break;
//                case SCRIPTA_ARCHIVIO_PERMESSI_DELETE:
//                    AttributiStruttura attributiStruttura = (AttributiStruttura) oggetto;
//                    TipologiaStruttura tipologiaStruttura = attributiStruttura.getIdTipologiaStruttura();
//                    idOggetto = tipologiaStruttura.getId().toString();
//                    descrizioneOggetto = tipologiaStruttura.getTipologia();
//                    tipoOggetto = Krint.TipoOggettoKrint.BABORG_TIPOLOGIA_STRUTTURA;
//                    KrintBaborgTipologiaStruttura krintBaborgTipologiaStruttura = factory.createProjection(KrintBaborgTipologiaStruttura.class, tipologiaStruttura);
//                    jsonKrintOggetto = objectMapper.writeValueAsString(krintBaborgTipologiaStruttura);
//                    break;
//            }

            krintService.writeKrintRow(
                    idOggetto,
                    tipoOggetto,
                    descrizioneOggetto,
                    jsonKrintEntitaPermessoArchivio,
                    archivio.getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO, // tipoOggetto
                    archivio.getNumerazioneGerarchica(), // descrizioneOggetto
                    jsonKrintArchivio, // informazioniOggetto
                    codiceOperazione);

        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = archivio.getId();
            } catch (Exception exa) {
            }
            krintService.writeKrintError(idOggetto, "writePermessiArchivio", codiceOperazione);
        }
    }

    public void writeArchivioDoc(ArchivioDoc archivioDoc, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto
            KrintScriptaDoc krintScriptaDoc = factory.createProjection(KrintScriptaDoc.class, archivioDoc.getIdDoc());
            String jsonKrintDoc = objectMapper.writeValueAsString(krintScriptaDoc);

            // Informazioni oggetto contenitore
            KrintScriptaArchivio krintScriptaArchivio = factory.createProjection(KrintScriptaArchivio.class, archivioDoc.getIdArchivio());
            String jsonKrintArchivio = objectMapper.writeValueAsString(krintScriptaArchivio);

            krintService.writeKrintRow(
                    archivioDoc.getIdDoc().getId().toString(), // idOggetto
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO_DOC, // tipoOggetto
                    archivioDoc.getIdDoc().getOggetto(), // descrizioneOggetto
                    jsonKrintDoc, // informazioniOggetto
                    archivioDoc.getIdArchivio().getId().toString(), // Da qui si ripete ma per il conenitore
                    Krint.TipoOggettoKrint.SCRIPTA_ARCHIVIO,
                    archivioDoc.getIdArchivio().getNumerazioneGerarchica(),
                    jsonKrintArchivio,
                    codiceOperazione);
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = archivioDoc.getIdDoc().getId();
            } catch (Exception exa) {
            }
            krintService.writeKrintError(idOggetto, "writeArchivioDoc", codiceOperazione);
        }

    }

    public void writeDoc(Doc doc, OperazioneKrint.CodiceOperazione codiceOperazione) {
        try {
            // Informazioni oggetto

            krintService.writeKrintRow(
                    doc.getId().toString(),
                    Krint.TipoOggettoKrint.SCRIPTA_DOC,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    codiceOperazione);
        } catch (Exception ex) {
            Integer idOggetto = null;
            try {
                ex.printStackTrace();
                idOggetto = doc.getId();
            } catch (Exception exa) {
            }
            krintService.writeKrintError(idOggetto, "writeDoc", codiceOperazione);
        }
    }
}
