package de.stromentlastung.antrag.vorgang;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import de.stromentlastung.antrag.common.CurrentUser;
import de.stromentlastung.antrag.config.SecurityConfig;
import de.stromentlastung.antrag.rechenkern.Entlastungsrechner;

@WebMvcTest(AntragController.class)
@Import({SecurityConfig.class, CurrentUser.class})
class AntragControllerTest {

    @Autowired
    MockMvc mvc;
    @MockitoBean
    AntragService antraege;

    @Test
    void die_vorschau_rechnet_fuer_jede_angemeldete_person() throws Exception {
        when(antraege.vorschau(any())).thenReturn(Entlastungsrechner.berechne(new Entlastungsrechner.Eingabe(2025, 1_850_000, 0, false, 0, null)));
        mvc.perform(post("/vorschau").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entnahmejahr\":2025,\"versteuertKwh\":1850000,\"elektromobilitaetKwh\":0,\"elektromobilitaetGeschaetzt\":false,\"nutzenergieDritteKwh\":0}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ANTRAGSTELLER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.festgesetztCent").value(3675000))
                .andExpect(jsonPath("$.zitate[1]").value("§ 9b Abs. 2a StromStG in der für das Entnahmejahr geltenden Fassung"));
    }

    @Test
    void die_freigabewarteschlange_gehoert_der_zeichnung() throws Exception {
        mvc.perform(get("/freigabe").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SACHBEARBEITUNG"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ZUGRIFF_VERWEIGERT"));
    }

    @Test
    void nur_antragsteller_legen_antraege_an() throws Exception {
        mvc.perform(post("").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entnahmejahr\":2025,\"versteuertKwh\":1000,\"elektromobilitaetKwh\":0,\"elektromobilitaetGeschaetzt\":false,\"nutzenergieDritteKwh\":0,\"versicherungKeineRueckforderung\":true,\"versicherungKeineSchwierigkeiten\":true,\"selbsterklaerungBeihilfen\":false}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SACHBEARBEITUNG"))))
                .andExpect(status().isForbidden());
    }
}
