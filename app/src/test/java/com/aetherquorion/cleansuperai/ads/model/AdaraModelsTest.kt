package com.aetherquorion.cleansuperai.ads.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AdaraModelsTest {
    @Test
    fun parsesAdConfigResponse() {
        val json = """
            {
              "pzdyeqenjrku": {
                "hktkevvwwbw": {
                  "hdvrhqxtjsr": {
                    "ntpwxttl": {
                      "xjcchmpevr": {
                        "udjpjamfklmglw": {
                          "beepiw": 200,
                          "lcpakw": [{
                            "aaojq": "ca-app-pub-test/test",
                            "vlthm": 2082349918929031200,
                            "dhdeit": "0",
                            "jxlwhu": 100,
                            "dgsup": 100
                          }]
                        }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val payload = Gson().fromJson(json, AdaraImportBean::class.java)
            .pzdyeqenjrku
            ?.hktkevvwwbw
            ?.hdvrhqxtjsr
            ?.ntpwxttl
            ?.xjcchmpevr
            ?.udjpjamfklmglw

        assertEquals(200, payload?.beepiw)
        assertEquals("ca-app-pub-test/test", payload?.lcpakw?.first()?.aaojq)
        assertEquals(2082349918929031200L, payload?.lcpakw?.first()?.vlthm)
        assertEquals("0", payload?.lcpakw?.first()?.dhdeit)
    }

    @Test
    fun parsesAppConfigResponse() {
        val json = """
            {
              "pzdyeqenjrku": {
                "hktkevvwwbw": {
                  "hdvrhqxtjsr": {
                    "ntpwxttl": {
                      "xjcchmpevr": {
                        "udjpjamfklmglw": {
                          "ljg": 200,
                          "pupyzt": {
                            "xnr": 30,
                            "yhanof": [{
                              "walj": "native_content",
                              "hqcz": "Open"
                            }]
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val payload = Gson().fromJson(json, AdaraInfoBean::class.java)
            .pzdyeqenjrku
            ?.hktkevvwwbw
            ?.hdvrhqxtjsr
            ?.ntpwxttl
            ?.xjcchmpevr
            ?.udjpjamfklmglw

        assertEquals(200, payload?.ljg)
        assertNotNull(payload?.pupyzt)
        assertEquals(30, payload?.pupyzt?.xnr)
        assertEquals("native_content", payload?.pupyzt?.yhanof?.first()?.walj)
        assertEquals("Open", payload?.pupyzt?.yhanof?.first()?.hqcz)
    }

    @Test
    fun parsesInstallUploadResponse() {
        val json = """
            {
              "bvuzgxbrfintde": "hfmgedco",
              "falupabkehapei": "jthydq",
              "pzdyeqenjrku": {
                "ykxzcbzn": "ejzkxrofttxa",
                "qgeuvrqxoe": "mrbojc",
                "hktkevvwwbw": {
                  "hdvrhqxtjsr": {
                    "exrfelapzsmorv": "vjysln",
                    "ntpwxttl": {
                      "pmvefkeqsndnzs": "uzzagbtpstj",
                      "xjcchmpevr": {
                        "unjbkifarzs": "rqoxuvah",
                        "udjpjamfklmglw": {
                          "evj": 200,
                          "vhj": null,
                          "axc": "success",
                          "success": true
                        },
                        "kibpelypie": "xuzgrtgpvnudia",
                        "fqdpwmojcgyu": "yvisps",
                        "rdyyzhrlytyeun": "lbrzozkkrveuug"
                      },
                      "rsxgvoqc": "fgjvdfvfkbxeyj",
                      "lpfrnoihukem": "idltksqxldkb"
                    },
                    "svtveiwsnjc": "kxasjaoadvrg",
                    "naxdjokqpdju": "voovkb",
                    "wmqfuv": "ryjgsevxolfwnh"
                  },
                  "kiribipuhc": "nhxnlp",
                  "jfwhzz": "jiobqjpuiho",
                  "ufkiho": "diwoaqcxsrd"
                }
              }
            }
        """.trimIndent()

        val payload = Gson().fromJson(json, UploadLogInfoBean::class.java)
            .pzdyeqenjrku
            ?.hktkevvwwbw
            ?.hdvrhqxtjsr
            ?.ntpwxttl
            ?.xjcchmpevr
            ?.udjpjamfklmglw

        assertEquals(200, payload?.evj)
    }
}
