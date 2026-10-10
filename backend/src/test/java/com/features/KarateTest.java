package com.features;

import com.intuit.karate.junit5.Karate;

class KarateTest {

    @Karate.Test
    Karate testAll() {
        // Isso vai rodar todos os arquivos .feature que estiverem na mesma pasta deste arquivo
        return Karate.run().relativeTo(getClass());
    }
}
