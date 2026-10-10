package com.features;

import com.intuit.karate.junit5.Karate;

class KarateTest {

    @Karate.Test
    Karate testAll() {
        // Lista explícita dos features de teste.
        // O setup-farmacia.feature não entra aqui pois é chamado via callonce
        // pelo clientes.feature — não é um teste independente.
        return Karate.run("clientes").relativeTo(getClass());
    }
}

