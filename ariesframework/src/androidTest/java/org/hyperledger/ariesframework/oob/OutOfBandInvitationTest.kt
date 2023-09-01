package org.hyperledger.ariesframework.oob

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.hyperledger.ariesframework.agent.MessageSerializer
import org.hyperledger.ariesframework.connection.messages.TrustPingMessage
import org.hyperledger.ariesframework.oob.messages.OutOfBandInvitation
import org.hyperledger.ariesframework.oob.models.HandshakeProtocol
import org.hyperledger.ariesframework.oob.models.OutOfBandDidDocumentService
import org.hyperledger.ariesframework.oob.models.PublicDidService
import org.hyperledger.ariesframework.util.DIDParser
import org.junit.Assert.assertEquals
import org.junit.Test

class OutOfBandInvitationTest {
    @Test
    fun testToUrl() {
        val domain = "https://example.com/ssi"
        val json = """
            {
                "@type": "https://didcomm.org/out-of-band/1.1/invitation",
                "services": ["did:sov:LjgpST2rjsoxYegQDRm7EL"],
                "@id": "69212a3a-d068-4f9d-a2dd-4741bca89af3",
                "label": "Faber College",
                "goal_code": "issue-vc",
                "goal": "To issue a Faber College Graduate credential",
                "handshake_protocols": ["https://didcomm.org/didexchange/1.0", "https://didcomm.org/connections/1.0"]
            }
        """.trimIndent()
        val invitation = Json.decodeFromString<OutOfBandInvitation>(json)
        val invitationUrl = invitation.toUrl(domain)

        val decodedInvitation = OutOfBandInvitation.fromUrl(invitationUrl)
        assertEquals(invitation.id, decodedInvitation.id)
        assertEquals(invitation.type, decodedInvitation.type)
        assertEquals(invitation.label, decodedInvitation.label)
        assertEquals(invitation.goalCode, decodedInvitation.goalCode)
        assertEquals(invitation.goal, decodedInvitation.goal)
        assertEquals(invitation.handshakeProtocols, decodedInvitation.handshakeProtocols)
    }

    @Test
    fun testFromUrl() {
        val invitationUrl = "http://example.com/ssi?oob=eyJAdHlwZSI6Imh0dHBzOi8vZGlkY29tbS5vcmcvb3V0LW9mLWJhbmQvMS4xL2ludml0YXRpb24iLCJAaWQiOiI2OTIxMmEzYS1kMDY4LTRmOWQtYTJkZC00NzQxYmNhODlhZjMiLCJsYWJlbCI6IkZhYmVyIENvbGxlZ2UiLCJnb2FsX2NvZGUiOiJpc3N1ZS12YyIsImdvYWwiOiJUbyBpc3N1ZSBhIEZhYmVyIENvbGxlZ2UgR3JhZHVhdGUgY3JlZGVudGlhbCIsImhhbmRzaGFrZV9wcm90b2NvbHMiOlsiaHR0cHM6Ly9kaWRjb21tLm9yZy9kaWRleGNoYW5nZS8xLjAiLCJodHRwczovL2RpZGNvbW0ub3JnL2Nvbm5lY3Rpb25zLzEuMCJdLCJzZXJ2aWNlcyI6WyJkaWQ6c292OkxqZ3BTVDJyanNveFllZ1FEUm03RUwiXX0K" // ktlint-disable max-line-length
        val invitation = OutOfBandInvitation.fromUrl(invitationUrl)

        assertEquals(invitation.id, "69212a3a-d068-4f9d-a2dd-4741bca89af3")
        assertEquals(invitation.type, "https://didcomm.org/out-of-band/1.1/invitation")
        assertEquals(invitation.label, "Faber College")
        assertEquals(invitation.goalCode, "issue-vc")
        assertEquals(invitation.goal, "To issue a Faber College Graduate credential")
        assertEquals(
            invitation.handshakeProtocols,
            listOf(HandshakeProtocol.DidExchange, HandshakeProtocol.Connections),
        )
        if (invitation.services[0] is PublicDidService) {
            assertEquals((invitation.services[0] as PublicDidService).did, "did:sov:LjgpST2rjsoxYegQDRm7EL")
        } else {
            throw Exception("Expected did service")
        }
    }

    @Test
    fun testFromJson() {
        val json = """
        {
            "@type": "https://didcomm.org/out-of-band/1.1/invitation",
            "@id": "69212a3a-d068-4f9d-a2dd-4741bca89af3",
            "label": "Faber College",
            "goal_code": "issue-vc",
            "goal": "To issue a Faber College Graduate credential",
            "handshake_protocols": ["https://didcomm.org/didexchange/1.0", "https://didcomm.org/connections/1.0"],
            "services": ["did:sov:LjgpST2rjsoxYegQDRm7EL"]
        }
        """
        val invitation = Json.decodeFromString<OutOfBandInvitation>(json)
        assertEquals(invitation.label, "Faber College")
    }

    @Test
    fun testLegacyProtocolType() {
        val json = """
        {
            "@type": "https://didcomm.org/out-of-band/1.1/invitation",
            "@id": "69212a3a-d068-4f9d-a2dd-4741bca89af3",
            "label": "Faber College",
            "goal_code": "issue-vc",
            "goal": "To issue a Faber College Graduate credential",
            "handshake_protocols": ["did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/didexchange/1.0", "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/connections/1.0"],
            "services": ["did:sov:LjgpST2rjsoxYegQDRm7EL"]
        }
        """
        val invitation = OutOfBandInvitation.fromJson(json)
        assertEquals(invitation.label, "Faber College")
    }

    @Test
    fun testInvitationWithService() {
        val json = """
        {
            "@type": "https://didcomm.org/out-of-band/1.1/invitation",
            "@id": "69212a3a-d068-4f9d-a2dd-4741bca89af3",
            "label": "Faber College",
            "goal_code": "issue-vc",
            "goal": "To issue a Faber College Graduate credential",
            "handshake_protocols": ["https://didcomm.org/didexchange/1.0", "https://didcomm.org/connections/1.0"],
            "services": [
                {
                    "id": "#inline",
                    "type": "did-communication",
                    "recipientKeys": ["did:key:z6MkmjY8GnV5i9YTDtPETC2uUAW6ejw3nk5mXF5yci5ab7th"],
                    "routingKeys": ["did:key:z6MkmjY8GnV5i9YTDtPETC2uUAW6ejw3nk5mXF5yci5ab7th"],
                    "serviceEndpoint": "https://example.com/ssi"
                }
            ]
        }
        """

        val invitation = Json.decodeFromString<OutOfBandInvitation>(json)
        if (invitation.services[0] is OutOfBandDidDocumentService) {
            val didDocument = invitation.services[0] as OutOfBandDidDocumentService
            assertEquals(didDocument.id, "#inline")
            assertEquals(didDocument.recipientKeys[0], "did:key:z6MkmjY8GnV5i9YTDtPETC2uUAW6ejw3nk5mXF5yci5ab7th")
            assertEquals(didDocument.routingKeys?.get(0), "did:key:z6MkmjY8GnV5i9YTDtPETC2uUAW6ejw3nk5mXF5yci5ab7th")
            assertEquals(didDocument.serviceEndpoint, "https://example.com/ssi")
        } else {
            throw Exception("Expected OutOfBandDidDocumentService service")
        }
    }

    @Test
    fun testFingerprints() {
        val json = """
        {
            "@type": "https://didcomm.org/out-of-band/1.1/invitation",
            "@id": "69212a3a-d068-4f9d-a2dd-4741bca89af3",
            "label": "Faber College",
            "goal_code": "issue-vc",
            "goal": "To issue a Faber College Graduate credential",
            "handshake_protocols": ["https://didcomm.org/didexchange/1.0", "https://didcomm.org/connections/1.0"],
            "services": [
                {
                    "id": "#inline",
                    "type": "did-communication",
                    "recipientKeys": ["did:key:z6MkmjY8GnV5i9YTDtPETC2uUAW6ejw3nk5mXF5yci5ab7th"],
                    "serviceEndpoint": "https://example.com/ssi"
                },
                "did:sov:LjgpST2rjsoxYegQDRm7EL",
                {
                    "id": "#inline",
                    "type": "did-communication",
                    "recipientKeys": ["did:key:123", "did:key:456"],
                    "serviceEndpoint": "https://example.com/ssi"
                }
            ]
        }
        """

        val invitation = Json.decodeFromString<OutOfBandInvitation>(json)
        assertEquals(invitation.fingerprints(), listOf("z6MkmjY8GnV5i9YTDtPETC2uUAW6ejw3nk5mXF5yci5ab7th", "123", "456"))
        assertEquals(invitation.invitationKey(), DIDParser.convertFingerprintToVerkey("z6MkmjY8GnV5i9YTDtPETC2uUAW6ejw3nk5mXF5yci5ab7th"))
    }

    @Test
    fun testRequests() {
        val invitation = OutOfBandInvitation("test invitation", services = listOf(PublicDidService("did:sov:LjgpST2rjsoxYegQDRm7EL")))
        val trustPing = TrustPingMessage("test")
        MessageSerializer.registerMessage(TrustPingMessage.type, TrustPingMessage::class)
        invitation.addRequest(trustPing)
        val requests = invitation.getRequestsJson()
        assertEquals(requests.size, 1)

        val request = Json.decodeFromString<TrustPingMessage>(requests[0])
        assertEquals(request.comment, "test")
    }

    @Test
    fun testParseLargeInvitation() {
        val url = "http://example.com?oob=eyJAdHlwZSI6Imh0dHBzOi8vZGlkY29tbS5vcmcvb3V0LW9mLWJhbmQvMS4xL2ludml0YXRpb24iLCJAaWQiOiJlODM1NzRjYy1kY2ExLTQwZmEtYWZmMS1kYzg0MDQzOWYzN2IiLCJsYWJlbCI6IkZhYmVyIENvbGxlZ2UiLCJnb2FsX2NvZGUiOiJpc3N1ZS12YyIsImdvYWwiOiJUbyBpc3N1ZSBhIGNyZWRlbnRpYWwiLCJhY2NlcHQiOlsiZGlkY29tbS9haXAxIiwiZGlkY29tbS9haXAyO2Vudj1yZmMxOSJdLCJoYW5kc2hha2VfcHJvdG9jb2xzIjpbImh0dHBzOi8vZGlkY29tbS5vcmcvZGlkZXhjaGFuZ2UvMS4wIiwiaHR0cHM6Ly9kaWRjb21tLm9yZy9jb25uZWN0aW9ucy8xLjAiXSwic2VydmljZXMiOlt7ImlkIjoiI2lubGluZS0wIiwic2VydmljZUVuZHBvaW50IjoiaHR0cDovL2xvY2FsaG9zdDozMDAwIiwidHlwZSI6ImRpZC1jb21tdW5pY2F0aW9uIiwicmVjaXBpZW50S2V5cyI6WyJkaWQ6a2V5Ono2TWtrZXZHemJ6VXNxRVBQeDdwUWJhbkNFNGNTZ2Z4WFBmeVpXU2NjbWd5RmlqRiJdLCJyb3V0aW5nS2V5cyI6W119XSwicmVxdWVzdHN-YXR0YWNoIjpbeyJAaWQiOiI4MDdhMzk1NC02ZjEwLTQ5YmQtYTI1My0zYTU0ZTdiZWRkN2IiLCJtaW1lLXR5cGUiOiJhcHBsaWNhdGlvbi9qc29uIiwiZGF0YSI6eyJiYXNlNjQiOiJleUpBZEhsd1pTSTZJbWgwZEhCek9pOHZaR2xrWTI5dGJTNXZjbWN2YVhOemRXVXRZM0psWkdWdWRHbGhiQzh4TGpBdmIyWm1aWEl0WTNKbFpHVnVkR2xoYkNJc0lrQnBaQ0k2SWpZeU5tRmhOV0ZoTFdFNU9HVXROR1l6WXkwNU5tTmlMVFUxTnpWa1pUY3lNV0ZqTkNJc0ltTnlaV1JsYm5ScFlXeGZjSEpsZG1sbGR5STZleUpBZEhsd1pTSTZJbWgwZEhCek9pOHZaR2xrWTI5dGJTNXZjbWN2YVhOemRXVXRZM0psWkdWdWRHbGhiQzh4TGpBdlkzSmxaR1Z1ZEdsaGJDMXdjbVYyYVdWM0lpd2lZWFIwY21saWRYUmxjeUk2VzNzaWJXbHRaUzEwZVhCbElqb2lkR1Y0ZEM5d2JHRnBiaUlzSW01aGJXVWlPaUp1WVcxbElpd2lkbUZzZFdVaU9pSkJiR2xqWlNKOUxIc2liV2x0WlMxMGVYQmxJam9pZEdWNGRDOXdiR0ZwYmlJc0ltNWhiV1VpT2lKaFoyVWlMQ0oyWVd4MVpTSTZJakl3SW4xZGZTd2liMlptWlhKemZtRjBkR0ZqYUNJNlczc2lRR2xrSWpvaWJHbGlhVzVrZVMxamNtVmtMVzltWm1WeUxUQWlMQ0p0YVcxbExYUjVjR1VpT2lKaGNIQnNhV05oZEdsdmJpOXFjMjl1SWl3aVpHRjBZU0k2ZXlKaVlYTmxOalFpT2lKbGVVcDZXVEpvYkdKWFJtWmhWMUZwVDJsSk0xTXpWa1ZXU0VKU1lVUk9TRk5xWkVoalJGcHlVbGhLZDFZeldrNVBha2syWXpKT2IxcFhNV2hNVjAxM1RrUnJNbHBVVlhoTVZFVXdUMGRSZEU1SFRtaE9VekZwVFhwSmVFeFVTbXRPZWxacVRsUkNiRnBxVlhwWlZHOTRUR3BCYVV4RFNtcGpiVlpyV0RKU2JGcHNPWEJhUTBrMlNXcGtUR1JWVWxWalJrWnZUVEJrUzA0d1pIZE9iWFJHWTI1Q1dHUnJNRFpOZW5CRVZFUnZNVTlVV1RST2VsVTJXa2RXYlZsWVZuTmtRMGx6U1cxMGJHVldPV3BpTTBwNVdsZE9NR0p0Vm5wak1UbDNZMjA1ZGxwcFNUWmxlVXBxU1dwdmFVNTZVVFZPYWxreFRVUm5lRTVFVFRSTmFrMHlUbnBOTkU1NlNUQk5ha1UwVG5wak1FOVVZM3BOUkdONlRXcE5NMDU2YXpST2FtTXdUWHBGZDAxRVRUUk9SRWw2VG5wQk1rOUVTWGxOUkdjMFRVUlZlVTVFUlhoT1ZFVjVUbFJWTVUxNlNURk9WRUZwVEVOS05HVnNPV3BaV0VGcFQybEplVTVxVFRKTlZFRTFUWHBWZVU1cWEzZFBSR3Q2VFVSSmVVNVVZekJPYWxFMVRrUlZNVTFVVFRST2FsbDRUbnBaZUUxVVZUTk9la0Y0VGtSak1rMTZTVFJQUkVsNlRWUkZNVTE2VVRCTmVtdDZUV3BqTWs5VVNUSk5SRTAwVFVSRmQwNUVRWHBQUkVrd1RucFJkMDFVUVRSTlJGRTFUWHBGTkUxVVRUUk5ha0V5VDBSVk5FOVVaM3BPUkVWNFRVUmplRTVVVFRCT1ZHTjRUa1JSZDA5VVVUSlBSRkYzVGtSak1FMVVWWHBPYWtsNVRYcE5OVTFVV1hoTlZHTTBUV3BSTTAxcVJUTlBWRWw2VG5wcmVVMUVSVEJPUkZFMFRWUmplazFFUlhwTlJGVTBUMFJCTVU5RVNYZE9SR013VG1wak0wNVVSWGxOVkVreFQxUlJOVTVFUVhsT2VtTXhUMVJqZUUxVVp6Qk9WRVV3VFZSak1VMVVVWGxPZWxWNlQxUlZNVTU2YXpGUFZFRTBUMFJGTlU5RVkzcE5WRmw0VFVSTk5VNTZXVEpOUkZFMVQwUnJNazVFU1RWTmFtY3lUbXBqTkUxNlVUVk9hbXN4VG1wVk5VNTZWVE5OVkVWNVRucE5lVTFFUlRGT1JGRXpUbFJqTkU1cVkzbE9SR00xVG5wTmVVMVVSVEJOZWxrd1RVUlpNRTU2U1hoTmFtZDNUa1JCTUU5RVNYZE5lbEV4VGtSbmVVNXFZM2hPUkVreFQxUm5NMDU2WjNwT1JFbDVUa1JyZDA1RVZUQk9lbU13VG5wTmVrMXFVVEpOVkdOM1RsUkZORTE2WnpCTlJFa3dUa1JWTTAxNlZYbE5la1V5VFVSUk1FOVVRVEZPZWtreFRVUkZORTFxVlRCT2VrRjZUV3BOTlU1VVNUTk5lazAwVFZSQk5FMVVRVEJOZWsxNFRYcG5NazU2WjNsT1ZHc3hUMVJaTTAxNll6Uk9SRWw0VGtSbk1FNXFZM2hPZWtFMFRrUlpNRTlFUVRGT2VrVjRUbnBSZUUxRVp6Sk9SRVUxVFVSVmQwNXFTVEpPYWxVMFRXcGpNazU2UlhkT1JFRTFUVlJaZUU5VVp6RlBSR014VDBSSk0wOVVTWGRPYWxrd1QxUkpNazVFV1RCTmFtZDVUVlJKTlU1cVl6Rk9hbFYzVDFSSmVrMUVZelZPYWxFMFRYcFJkMDE2VlhkUFJFa3dUa1JWTWsxRVozaE5ha0V4VFhwSk5VNTZSVFZOYWxFd1RsUmpNazU2UVhwT1ZGVXpUWHByZWsxcVl6Sk9WRmswVDBSTk1VNUVUWGRQUkdzMVRWUlZlazU2YXpWT1JHc3hUMFJSZWs5RVdUVk9ha0UxVFZSQmVVNXFZM2hPZW1jd1RWUlJNRTlFWXpCT2FtTXlUbnBKTUU1VVFURk9hbEYzVFZSTmVVNXFRVEJPZWxVMFRtcGplRTVxVVhoUFZGRXlUVlJuTWs1VVRUVk9hbWQ1VFhwRk1FOUVRWHBOZWxFeFRWUkpkMDU2UVhwT2VsVjRUMVJWTlU1VVVUTlBWR3QzVGtSQk0wNTZXVEZPZW1NelRYcEZNMDE2YXpKUFJGRXpUbnBWTUUxcVVUUlBSR2Q2VGxSRmFVeERTalJqYkRscVdWaEJhVTlzZEdKSmJUVm9ZbGRWYVV4RFNYaE5lbGswVGtSTmVVMVVUWHBOZW1kM1QxUkJOVTlFUlhoTlZGazFUV3BaTkU5RWEzcFBWR2MxVG1wUmVrOUVSVEJPUkVVelRrUlZNRTVFUVhsTlZGa3dUbnBWTWsxRVkzZFBSR00xVFdwVk0wMXFhekZOVkZreVRXcGplazlVVlhwTmFsRXhUbXBGTkU1cVFUTk5la2wzVFVSSk0wOUVXVEZOUkVWNlRWUkplRTVVU1RGT1ZFVXdUV3BCTWs1RVdUTk5lbU0wVG5wcmVVNTZZelJQUkdzelQwUkZORTVVYTNkT2FtdDVUbnBWTkU1VVNUSk9SRkY2VG1wUk5FOVVaM2hOUkZGNVRVUkJNRTVxUVhsUFJFbDRUVlJqTWs1cVl6TlBSRTB4VDFSQmVFNTZWVEpOYWtFMVRVUm5lazlVUVhoT1JFVjVUbnBCZUUxRVNUSlBSRlV5VDFSWk1VMVVXVEZOZW10M1RucG5NVTVVVlhsT2VrMDFUV3BGZWs1RVkzcE5hbGwzVFVSUk5VOVVXWHBOZW1zeVRrUkZkMDE2VVhsUFJFRXhUbnBaTTA1Nll6Vk9lbXQ0VG5wUk1rNTZRWHBPVkUwd1RrUlplRTFFYXpOTlZFbDNUbnBGTWsxNlVYcE5WRVY2VDBSUmQwNVVUWGxPYW1ONlQwUlJOVTFxUlRKT2VrRTBUVlJaZUU1cVFUQk9WRVV3VGtSVmVFMVVTVEJOYWxVd1RVUm5kMDE2UlRKUFJFa3hUVVJGTkU1cVNURk9SR2N6VG5wUmVFNVVTVEpOVkdONlRVUm5lRTVxYTNkTlZFRjZUWHByTTAxVVNUTk5WRVY2VFdwUmVVOUVRVFJPYW1zd1RWUkpORTU2UVRST2VsRTFUbFJOZVU1NlkzbFBWRTE2VGtSbk1rOUVZM3BOYW10NlRWUmplVTFFUVRSTmFtTjNUMFJGZDA1RVRUTlBSRmw0VDBSWk0wMTZZelZOVkVGNlRtcG5NVTVFYXpOUFJGRXhUbnBSTVU1RVdUVlBWRlV6VGxSRmQwNXFWVFJOUkVreFQwUlJNVTlVWTNoUFZHc3hUVVJSTUU1cVdUUk9lbGt3VG5wUmVrNUVVWGhQVkdONVRYcEpNMDE2U1hoUFZHY3dUV3BaTkUxcVZUTk9WRkV5VDFSQk5FMTZUVFJPVkZrMVRYcHJOVTVFWnpOTmFtY3hUMVJGTTA1NldYZFBSR3N5VFdwTk0wOVVTVFZOYWtsNVQwUlZNRTVxVVhwUFZHczFUMVJWTWsxVVRURlBWRVV6VGxSSmVrMXFaM2xPZWtWNFRWUnJORTE2UVRSTlZGVXpUa1JyTUU1VVp6Rk5la0Y0VFdwcmVVNTZWVE5PUkdNMFRXcEJOVTlVUlROT2FsRXpUa1JyZVU1cVJYZE9WRkYzVFdwWmVrNTZaM3BQUkZWNFRVUlpNVTU2V1hkT1JGbDVUWHBKTVU1RVZUVk5WRWswVDBSUk0wNUVSVEpPZW1NMVRrUlZNazVxVlhoTmFtZDNUa1JSTUUxVVJYcE9WRmsxVDFSTmVVNVVUVEJPUkVGNFRtcEpORTlVVVRKT2FrMTVUbFJyZUU5RVFYbE9hbU15VFhwVk5VOVVXVFJPUkdzMVRWUk5lVTlFUlRWSmJEQnpWM2xLZEZsWVRqQmFXRXBtWXpKV2FtTnRWakJKYVhkcFRrUlZNVTVVVlRKTmFrMDBUMFJSTTA5RVJYaFBSRTAwVFdwWk5VOVVSVEZOVkdNeFRrUlplazlFV1hkTlJHZDVUbFJOTlUxcVl6Uk5lbEV4VGxSQmVVNUVTWHBOVkUxM1QxUkZNazU2VlRWUFZFa3pUVVJqZWs5VWEzcE9SR00xVG1wTmVrNTZTVE5PZWtsM1RXcGpORTFFWjNkT1ZFazFUMFJuTlU1NmF6RlBSRUV3VG5wQk5FMVVZM2xOUkVrMVRWUlJlVTFxVVRKT1JFbDRUMFJaZVU1NlRYcE9WR3Q0VG1wSk1VMVVSVFJPVkZGNVRtcFZlRTlVUVhsT1JGRjRUVVJSZDA1RVZYcE9SRTE1VFZSQk1rMTZZelJOUkUwMFRrUm5lazlFU1RKT1JHTjNUMFJGZWs5RVRUUk5SRkV4VFVSck5FMTZZekJOVkUwelRXcHJORTlVUVRWTmFsRXhUV3BKTVUxRVdYZE5WRUUxVFdwbmQwNUVSWHBOVkdNeFRsUlJOVTU2VFhoT2Vra3hUVlJaZVU5VVJYZE5lbU13VDFSSk1FOUVaelJOYW1zd1RucFZlazlFUlhsUFJHZDNUbXBKTWs1NlVYaFBSRmwzVDBSQmQwNUVSVEpPYW1zd1RsUkZNMDE2UlhsUFZHZDVUWHBWTlU5RVFYbE9WRTB5VGxSbmVVNXFXWGhOZWtWNFRrUm5lazFFU1ROUFJGVTBUbXBuTWsxcVdUTk9WRkUxVFdwSmVFMTZTWHBQUkZGM1RXcGpNRTE2UlRGUFZHZDZUV3BOZWs1RVZUVlBSRkV4VFZSSk1VNVVVVE5PVkVrd1QxUk5lazlVUVRSTlZHc3lUMFJOTTA1VVNUVlBSRlY1VDFSUk1FMVVhM3BPVkVrMVRWUkpNVTVxUVRGT2FtY3dUV3BGTlU1VWF6Rk9hazB6VGxSbk1rNUVWVFJPYW1NeFRucEJlRTlVWTNoTlJFMDBUMVJaTkU1cVFUUk5lbFY0VFhwbk5FMUVSWGhPYWxreFRVUlZNVTU2VlhwT1JFMDBUbFJSTWsxNlFYcFBSRWt4VFZSbk0wNTZRVEpPZWtVd1RWUlZNazVVWXpCTmVrRjNUMVJaTkUxRVJUQk9SRVV3VFdwRk5FNUVTWGxPZWxVelRsUlZlVTE2VVRWT2Vsa3dUWHBWTlUxRVFURk5WR3MwVGtSbk1FNVVSWGhPYWtGNlRsUm5lazU2WTNkTlZHTTBUVlJyTVUxcVRYbE5WR3N6VGxSVk0wNXFXVFJOUkZWM1QwUlZNazlFWXpCTlJFbDZUbFJKZDA1RVl6Sk9SRWw2VFVSamQwMTZUWGRPVkZreVRsUkpNRTFxVVhsT2FtczBUbnBGTlUxNlRYZE9hazAxVG5wVk1FNTZUWGxOUkUwMFRsUk5OVTFFVlhwTmVsRXdUMFJyTWsxNlNUSk9SRWw0VFZSak5VOUVhek5PUkZVMFQwUk5kMDlFV1RST2VsVXdUMVJaZUUxcVkzaE5SR016VFVSQmVFOVVUWGRPUkZWNFRtcG5OVTVxVlRST2VsbDVUVlJCTUU5VVVUSk5SRWw2VDBSak1rOVVVWGhPZWsxNlRsUmplRTVVV1hoUFJGRXhUbXBCZUUxRVkzaE9WRVV3U1d3d2MxZDVTbWhhTWxWcFRFTkplRTFxVFhkTlZHczFUbnBCTlU5RVFUQk9ha2sxVFdwWk0wMXFRWGhQVkUwd1RXcG5NVTFVWXpCT1ZFVXpUMVJCTUU5RVZYZE9SRUV6VGtSSmVrOVVXVEJQVkVVMFQwUmpOVTlFUlhoUFJFVXhUWHBSTUUxRVNYcE5SR2N6VFVSak1VOVVXVFJPYWxWNlQwUkZkMDFxWXpOTmFrbDZUa1JuTkU1VWEzcE5WR3N6VFdwVmVVMTZXVFZQUkUxNVRWUlZORTFVV1ROTlJHTjNUV3BuTkU5VVJYcE5hbFV6VFVSbk1VNUVWVFZPUkUxM1RucHJlRTU2V1RGT1ZHZDVUWHBqZWs5RVFUQlBSRTE2VDFSak0wOVVWWGhOUkZFelRXcG5kMDU2U1RKT2FsRXpUbFJyTVU1NldUTk9lbXN4VGtSak1rOVVZM2hQVkZVeFQxUmplRTU2UVRCTlZFMTVUbnBSTWs1VVVUUlBSRmw2VG1wUk1rNUVZekJPYW10NVQwUkZOVTFxYXpSTmVtczFUWHBuTlUxVVVUUlBWRVY2VG5wWk1rNUVRWGRPVkVVeFQwUlJlazFFUlROT1JHc3lUbFJaTTAxNlkzcE5SRlV5VFVSSk5VOVVSVEpQVkZVelRYcFJlazFxWnpGT1ZFVjRUbXBOZUU5RVRUSk9SR2MwVFhwTmVFNUVTWHBQUkdzeFRrUmpNRTVFV1RGTlZHZDVUVlJGTTA1cVRYbE9ha0UwVFZSQk5FOUVUVEJPUkZVelQwUkpNVTVVUVhsUFZFVjZUVVJWTlU1VVl6Rk9WR3N5VG5wTk1VMUVXVEZPUkZFeFRsUlplazlFYTNoTlZHYzBUMFJqTkU1VVFUUk9WR016VFdwSk0wNTZUWGhPYWtrMFQxUkZNMDFxUVRCT1JFMHlUbFJWTWs1VVkzaE5la0YzVG5wWk0wNXFUVFZOUkVrMFRVUkZlazE2WXpKUFZFVjRUMVJSZWsxRVkzaE5SRlY2VGxSVk5VMVVSWGROUkdzeVQxUkJlVTVVUVhoTlZHY3lUMFJqZVU1cVNUTlBSRVYzVFdwRmVFOVVUVEJQUkZrd1RsUk5lRTVxWTNoT1JFMTVUVVJKTTA1RVRUSk9WRUUxVG5wSmVFOUVVVE5OYW10NVRXcEJNRTlFUVRWTmFsRTFUbFJuTVU1NlJYbFBSRkUxVFhwRk1rMVVXVEJQUkdNd1RXcGpNazVxVlRGTmFrRjNUWHBKTkU1VVozcE9WRVUxVDBSTk1FMVVWWGRQUkdzd1RVUkJNVTFVUVRGT2VrVTBUV3BKZDAxNll6Qk9SR2Q0VFVSbk1rMVVVWGxQVkdkNFRXcGplVTVVVlRCUFZGVXdUa1JWTTAxRVJYaE5SR014VFVSVk0wNVVhekJQVkVrMVRrUlZNVTFFWjNwT1JHY3pUbFJCTlU1VVJYcE5lazB4VFdwSmVVNUVZek5PUkZsNVQwUmpNVTE2UVROT2VtY3lUV3BqZDA1NlZUTk9hbGw0VGtSSmVVNTZWVE5OVkUwelRVUnJlRTFxVVROT2VsRTBUVlJuZDAxNlJUUk5hbFY0VDFSbk0wMVVSVFZOYW10M1QwUkJlazFxVFROT2Fra3lUVVJOTTAxRVl6Rk9SR00xVGxSak1VbHNNV1JtVTNkcFltMDVkVmt5VldsUGFVa3dUMFJCZWs5VVJYZFBSRmt4VDBSWmVFMXFVVEZOUkVFd1QxUkplRTVxWjJsbVVUMDlJbjE5WFgwPSJ9fV19" // ktlint-disable max-line-length
        val invitation = OutOfBandInvitation.fromUrl(url)
        assertEquals(invitation.goalCode, "issue-vc")
        assertEquals(invitation.requests?.size, 1)
    }

    @Test
    fun testJsonEmbeddingInvitation() {
        val url = "http://127.0.0.1:8000?oob=eyJAdHlwZSI6ICJodHRwczovL2RpZGNvbW0ub3JnL291dC1vZi1iYW5kLzEuMC9pbnZpdGF0aW9uIiwgIkBpZCI6ICI2Y2E1ZDQ1NC1lNzdiLTQzMmItOThiOC01NDA4ZGFkZWJlMDIiLCAicmVxdWVzdHN-YXR0YWNoIjogW3siQGlkIjogInJlcXVlc3QtMCIsICJtaW1lLXR5cGUiOiAiYXBwbGljYXRpb24vanNvbiIsICJkYXRhIjogeyJqc29uIjogeyJAdHlwZSI6ICJodHRwczovL2RpZGNvbW0ub3JnL2lzc3VlLWNyZWRlbnRpYWwvMS4wL29mZmVyLWNyZWRlbnRpYWwiLCAiQGlkIjogImZiYzVhNjNlLTE2ODktNDYxNi05NGNmLThkZTE4ZmZkMjQ5NSIsICJ-dGhyZWFkIjogeyJwdGhpZCI6ICI2Y2E1ZDQ1NC1lNzdiLTQzMmItOThiOC01NDA4ZGFkZWJlMDIifSwgImNyZWRlbnRpYWxfcHJldmlldyI6IHsiQHR5cGUiOiAiaHR0cHM6Ly9kaWRjb21tLm9yZy9pc3N1ZS1jcmVkZW50aWFsLzEuMC9jcmVkZW50aWFsLXByZXZpZXciLCAiYXR0cmlidXRlcyI6IFt7Im5hbWUiOiAibmFtZSIsICJtaW1lLXR5cGUiOiAidGV4dC9wbGFpbiIsICJ2YWx1ZSI6ICJBbGljZSBTbWl0aCJ9LCB7Im5hbWUiOiAiZGF0ZSIsICJtaW1lLXR5cGUiOiAidGV4dC9wbGFpbiIsICJ2YWx1ZSI6ICIyMDE4LTA1LTI4In0sIHsibmFtZSI6ICJkZWdyZWUiLCAibWltZS10eXBlIjogInRleHQvcGxhaW4iLCAidmFsdWUiOiAiTWF0aHMifSwgeyJuYW1lIjogImJpcnRoZGF0ZV9kYXRlaW50IiwgIm1pbWUtdHlwZSI6ICJ0ZXh0L3BsYWluIiwgInZhbHVlIjogIjE5OTgxMDA3In0sIHsibmFtZSI6ICJ0aW1lc3RhbXAiLCAibWltZS10eXBlIjogInRleHQvcGxhaW4iLCAidmFsdWUiOiAiMTY2NTEzMDkxOCJ9XX0sICJvZmZlcnN-YXR0YWNoIjogW3siQGlkIjogImxpYmluZHktY3JlZC1vZmZlci0wIiwgIm1pbWUtdHlwZSI6ICJhcHBsaWNhdGlvbi9qc29uIiwgImRhdGEiOiB7ImJhc2U2NCI6ICJleUp6WTJobGJXRmZhV1FpT2lBaVUxaFpSVE5wUWxOWGNrWTRhbWcxTjIwMGVXaG5ZVG95T25OamFHVnRZU0JrWldkeVpXVTZNakF5TWk0eE1pNHhNeUlzSUNKamNtVmtYMlJsWmw5cFpDSTZJQ0pUV0ZsRk0ybENVMWR5UmpocWFEVTNiVFI1YUdkaE9qTTZRMHc2TmpJd016azBPakl3TWpJdU1USXVNVE1pTENBaWEyVjVYMk52Y25KbFkzUnVaWE56WDNCeWIyOW1Jam9nZXlKaklqb2dJakV3T1Rrek5ETTVPVEkzTXpZek1USXdOell4TnpJd05ESXlPRGt6TlRRNE56STFPRGs1TlRZM05Ua3pNRGM0T1RFd09EQTNNakUxTWpNek9UVTNORE15TlRZeE1UUXdOelEzTURjeE9TSXNJQ0o0ZWw5allYQWlPaUFpTWpNM056azRNak0xT1RrM01UZ3lOVEU1TURFeU16STJNemcxTkRrek9EY3lOemd5Tmpjek5EWXlOall5TlRVMk1UY3hPVEV6TWpFd05qTXdPVFE0TnpFNU5UYzBOalV5TmpjeE5qVTRNREUwTlRNM09UY3hNams0TWprME9EVTBPRGN5TlRRNE5EazBNVFExTXpnd056WTNOakV5TmpNNU16YzNNalkzTnpZd01EVTVNamcwTURVMk1qWTVOVEk0TlRNeE9UVTROekkxTkRVME56a3lOall3TlRjMk9UazBPRGsyT1RRM01ERXhOakUwTVRjeE5qVXhOVEF3TVRrNU5qWXpPRFl4T1RReU1ESTFNamd5TVRVME1EQTRNamczT0RjNU9USXlNVFF4TXpjeU56YzVOekEyT1RnM09EQTVPRGd6T0RZek1EWXhPREk0TVRFd016WTFOREV4TURBNE5EVXdOalEwT0RBM05UYzFNamcyTlRrek1EQXpORE13TVRneE16Y3pNRFUxTURJNU1UTXhOVGMzT0RJME1URTVOREV6TVRrME9UUTJOell5TlRRM01qY3pOVEk0TlRjeU9UWXhOelV4TkRRME5UTTBPRGsyTlRnMU5ESXlNVE16TmpNME1URXpPRGt5TVRZM01qVTJOelk0TlRJMU1EQTVPVFExT0RFM09EQTBOall3TWpZeU1UQTVNalE0T0RJMU1qZzROemcwTkRRME1EZ3dOakk0TlRBNE1ERTBNamt5TnprM01qY3hNakV6TWpZNE9ETTBOekF4T1RrNU1qUTNNREEyTmpNeE5UZzJNakUyT1Rnd09URTBNekV5T0RrM05UY3pNRGd4TkRFME1qSTRNamM1TWpZNE5qQTBPVGs0T1RZd01qUTFPRGt3TURNek1UTTBOemd6TkRrNU9URXlOelV3TkRZeE5EVTBNREl5T1RNeE9ETTRORGcxTURJME5UVXlPRGN6TWpNNU5EWTROamc1TkRZME16QTJPVFUzT1RFNE16WTNPREV4TkRrd05qTXhPRE15TkRJM05ERTVNemcyTnpVek9ERXlNekkyTmpNNE5UVTRPRGd6TVRFeU9ERTVOamcyT1RrMk56WTRPVFl4TlRnek56Z3lOREUwTnpRMk1UY3hPVGswTXpJNU1UUTBOemd6TXpNeE5qTTBOVE00TURNM09ERTVOakV5TnpBMk5USTFPVEkwTlNJc0lDSjRjbDlqWVhBaU9pQmJXeUowYVcxbGMzUmhiWEFpTENBaU9UazRNamN6TkRRME5qTXpOakk1TmpnMU5UZzROamszT1RrME1UTXlOREl3TkRrNU9EQXdNRGd4TXpVeU9USTFOVGcyT0RVME5qY3hOalk0TnpVeU16STRNRFl3TmpVMk56RTNNVEV6TWpNd05EUXlNelV6TlRRNU16SXhNalkzTkRZNE1ETTJORGcxTXpNME5UVTVNamt6TVRrMU1UazNORGsyT1RJMU9ESTRPVE13TmpreE5UQTFOekl6TmpVNE1qWXlNekExTURneU9Ea3hNREl6TmpReE56YzNNakUyTVRFeU16RTROell5TkRrME9UUTRPVEkyTWpZeE5Ea3lORE14TVRjM05EQTFNemsyTXpNNU5ESXlPREl3TlRFMU16ZzROakF4TkRrNE5UUTVOelk0TWpRNE9EVTRNVEkxTmpZMk1EUTNOek14TnpFd05URTJPVFk1TURReE5ERTBNVGc0T1RZd05qWXdOVGs0TVRrMU1qY3lNakUwTXprNU56STRNVEEyT0RZM056Z3pNekV4TkRneU1qazNPRGcyTURrME56YzVOekkyTVRnM01UZ3dNems1TURrNE5EUTFNRE0zT0RreE1EQXhPVFV6TXpnME5UZ3hNemMyTnpBMU5qYzBNams0TkRreU1USTFNamd6TlRVME1UVTJOVFl3TmpNNE9UWTBOalUyT0RjME1UYzJNVEUzT1RrME9EQXdNemMxTURnMU5UQTJPVGswTkRZMk9ERTROalF4TkRNeE1UVTNORGt6T1RVeE5EZ3hOVFUyTlRFNU5qZzBNREU1T0RZNU1EWXdOelUyTWpNNU5qTTFNVEF4TURNek1qZ3dNekU0TkRRNE1URXlPREl3TVRrMU9EZzVOelk0TURVNU9UZzVORGcwTVRVM05EQTJPVFk0TkRFMU1ERTBOVE00TWpneU1URTFOVGd5TlRBeU9UVTFPRE16TXpjeE9USXhOemc0TkRFM09USTFNalV6T0RRME9EQXdNemM1TkRVME16YzBNak16TWprd016STBNRGd3T1RZME1qQTFNRGc0TlRVNE1UVTVOREEyTVRjeE9ERXhOVE0zTkRJME1qVTFOemcwTWprM09EYzNOVGMwTlRNeU9UTXpNakE0TnpZNU1UUTVNVFUwTURBd056TTNORE0xTmpZM01EY3dPVFV4TVRjek5UWTNOVGd3TWpZMk1qSTNPRFV6TmpZaVhTd2dXeUprWldkeVpXVWlMQ0FpTWpNM09UTTFNVFF3TWpZNE5UTTJNVFF3TmpRM01UYzROVFE1T0RVNU1qVTFORFF6TlRFd016azBPVEV3TWpBeU9UZzNNakl4TkRNM056a3lOakkxTVRrek9EazJORFF5TWpZNE9ERXpNek15TlRjeU16QTJOalF5TnpNNU56QTVOelV5TVRFeE56YzVOVEV3TXprMU5UYzBOVEE1TXpZMk5UVXdOVFkwTURRd01EazFNRFEyT0RRNU16QTFNVFEwTWpNek1qazBOemt5T1Rnd01EUXdPRGcwT0RNek5qRTRPVEV4TWpZME5EQTVNRFl4TXpBeU1qTXhNVE00TWpVd01UWTNOamsyT0RRM01EVXpOVEV6TURVM09URXhOamN6TURnNU16RXlNVFl4TkRRMk5ETTFNRGt5T1RVeE56STFPVGMxT1RrMU5ETXpOemcwTURZMU9EWXhOekl4TkRZM01USTNOVGcxTURFeU9UWXdOVFl3T1RNM01UQTRNak0wTVRBM016Z3pNVFUzTkRFNE5qRTJNRFV4TmprME1qazFNalV5TmpnNU1UVXhOalF4TlRVME5EUXhNREF4TkRNeE56RTJNVEF5TVRFeE9UazFNelUxTkRRM05EVXpOalF6TURBME9EWTJOVE16TVRVeE5USTNPVFEyTWpjMU5UZ3pNVGszTXprNE9EYzFNekl5T0RneE5EUXdOVFU1TWpneE9EY3pOakk1T0RVek1UYzNNek0yTVRneU1ERTVNRFEwTnpZNE5EWXlORGswT0RBMU16STBOell3TnpJek9UY3pOems0TXpReE1UTTFNamM0T0RnNE1UTTRNakE0TXpVd05qQXpNakEzTURFek1EVXhOemMwTmpneE9UZ3dNakUyTURVM056TTJNVFE0TWpRMk1USTROakU0TWpRMk16UXpOemMxTXpjeU1qTTVORGt4T0RnMU5qWTNOalF3T1RBMU1USTNOVFE0T1RVME5qZzBNalU1T1RrNU1UZ3dOVFEwT0RjNU9EWTRNVE0xTnpNd09EWTJNRFE1TURZMU5UQTRNek0yTlRJNU16YzJOVEEzTWpVME5ESTBOREEzTlRZME1qVXlOelV3TmpRNU9UVXhNRGcwTVRrMk1qQTFOVEUwTXpZME16VTJOVE16T1RNeE56azRNVE0wTmpjNE5ETTVOVE0zTkRZNE5EQXpOREUwT0RFMk5qUXpNamM1SWwwc0lGc2laR0YwWlNJc0lDSTROemM1TlRVNU1UUXlPRFV6T1RJNU5EZ3pOamd6TURZd056UTVNalUxTXprek16QXlNRGs0TWpFM01USXpNRFV4T1RjM01UUTFORE0wTURNME16RXpNVGt6TnpVek1ESTVPVFE0TVRJNU5qQTVPRFUwT1RNMU5UTXdOalV3T1Rnd016azFOREExT0RZNU9URTNORFUzT1RBeE16STBPVGczT0RrNU1UUXhOalV3TmpjeU5UWXhPRFl5TXpNeE16UTFNVEEwTVRJNU1qVTJNVEV6TlRjMk5UQTVNemN5T0RZMk5EUXlNemsyTURRNE9ETTNOVFkzTWpjMk16TXpNREUxTlRJMk5UUXdPVGt5TkRjNU16UXlNekEzTVRNek56VTNPRGczT1RNMk5UZzJPVEF5TlRRek1ESTJORGd6T0RRME5qQXlNVFl5TURVNU5UUTVNVEUwT1RRME9EQXhNell4TlRnME9ESXhORGd3TlRBek9EWTVOVGd3TnpBNU1UQTRNalkzTmpjMU1EVXdPRFUzTkRjM056Y3hOelkzT1RZeU5UTTVOalV6T0RFM05ERTRPRFExTVRRME16Y3pOVGcwTkRFNE56QXdNakF6TnpFM05EazFOamN4TlRVeE1USTFOVGc1T1RNd09USXdNemt6TmpNek9EazBOemd6TURjNU9EWXhPVFExTURrNE5URTRPRFl5TVRVeE1qSTFPVFV6T0Rnek5EZ3lORGt5T0RRM016TXhNVGd4TURJNU5EQTFNRGN5TXpJMU1UazVNall5TnpnM05qZzNNakkyTXpVMU1qSTBOekV3TXpjeE5UWXhPVFkzTlRZek16UTFNRGMzTlRZeU1Ea3dOVEUwTURRd09UUXhNVEkyTmpJMk1EWXlOelV4TlRNNE1qazVORFl3TVRZNU16VXlNamc0TlRnMU56WXdOekUzTnpVME16TTBNVEkyTVRVMk1UWTFOVEEwTWpnM01EZzFPVEEzTWpjME5qRTRNVFkyTmpJeE5Ua3lOall5TkRNM01UZ3hPREU1TVRjNU56Z3lOVGs1T0RjMk1ESXpPRGd6TmpVeE1qTTBNREF4TURJNE9URTROemMzTmprNU5EWTRNVEl4TVRNd01qZzFNVGd4TlRZeU5qSXlNakl3TmpjNE1Ua3hNekE0TlRnNE16UXlOemMxT0RnNE5qUXpPREl4TlRReE1ETTJOVEV3TnpVd01EVWlYU3dnV3lKdVlXMWxJaXdnSWpjNU9EQTFNamt5T0RVeE5qUTJORE0yTmpRd09UTXhNRGd4TnpZNU1qa3pORGcwTkRZeU5USXpOREEzTURnM05qQTVOak13TkRjMk5qY3pPVGcyTnpBNU1ETTJOakkyT1RJMk5UTXlNREE1TVRNNU1qQTJOalF4TXpNNU5UY3pPVEE0TURRME5UWXlOekUxTkRBeU16RTNOVGcxTVRNM05UTTVNVGs0TWpBMk1qZzJPRFF6TXpreU5EQXlNVEl4TlRJeE1qSXlOamMzTlRJME5qYzRNVGN6T0RRMU1ESTVNakEwTWpjMk16Z3lOalV4TWpRM01qTXhNVGt4TnpBME9UWXdNVFV5TXpZME5qZ3dPVGs1TkRRM01UUXpOemN6TVRFd01ESTROakEzTXpBek56azJNVGMxTURNM05Ua3lPVFF6T0RjM01EUTJOak0yTXpjM05Ea3dNak0yTXpRME5qWTRNVEV4TlRrNE5EVXpNRGt3TkRneE5qa3pOVFkzTURjMU9ERTBNelV6TURjek5qUTBNekEwT1RZeE5qRTBNRE0wTURjek5EWTBNakExTnpJek5ETTNPRFkyTVRNeE1qSXhORFF6TWpRek5EQXhNalUyTXpZMU5EUXhNalUyTkRJd01UYzNNVFE1TWpBM016RXdOakUyTmpFeU5qZzVORFE0TXpZNU5qVTVNelUyT0RZNE1UUTFPVGsxTXpjeU9UWTVPRGc1TnpJd016WTFNelUyTkRBM056VTVOemMxTkRrM01URXlNVFk1TlRZME9EZ3dOelU1TWpnMk9UQXhORFEzTXpRNE9UY3hNRGN6TmpnME1Ua3pOVGM1TVRrME5ESXlNak15TWpRNU1UQXdOamc0TnpVeE56SXpOakl3TlRRNU5EVTFNRFF6TlRVMk1qVXdPRFl4TmpRd09ERXlNekkwTkRjME9UTTNOVFkzTnpnME9UQXdOREk0TXpjeE9UQTNNVE0yTmpneE5UWTFOak0xT1RBd05EVXlNelExT0RNNU9UWXpNelkyTmpjd01qazJNRFUwTWpReU9ERTJNelk1TkRJM09UQTBNVGt5TVRVM056Z3dNVE13TkRJM056TTVPRGszTWpFMk9EY3lNVGcxTmpjeE1UazJORE15T1RBNU9UWTNPVFF3TURFeU16azNPVFV4TXpVNU5qYzJOelExT0Rjd09UUXdOalU1TnpVNE16TTVNelF3TkNKZExDQmJJbUpwY25Sb1pHRjBaVjlrWVhSbGFXNTBJaXdnSWpZeU5qSTVOVEE0TmpjNU9UVTBNVFU1TVRZeE5UUTBOalkzT0RJd01qQTFOVFF5TVRRNE9EQXdNVGs0TVRVMU56ZzFNamcxTURBMk9UTTRNVGt5T0RreE1UWTVNall5TnpJek16TXpORGMyTXpneE1qRTFNVFV4TkRZNE1qY3lNVEk0TlRjeU56Y3lOREkwTVRBeU9UVXhNek0zTmpNMk9EWXlOVEEwTnpJM05UZzVNVGd4TVRFME9ESXdNelV3T0RreE16VTJPVFU1T0RBeE1Ea3pOekkyTXpFMk5UWTJOVE16TkRVNE56TTJPREU1TlRRNU9EazVNakV6TkRVeU1USTNOak13TkRjMU5ERXlOekUwTnpZNE16UXlPVGsxTWpReU5EQTBOVGMwTmpBNU16ZzRNekU0TURVNE56QTRNVEl5TmpBeE16WXpOekU1TlRjd056UTVNamswTkRNeE16RXlPREF5T1RjMU1UazBOVEV4TkRrd01qazFOalk1TnpJeE9EVTNNVGsxTXpJd05qRXdORFEzTlRRek5EQTNOVGN5TmpjM01ETXlOVFl5TWpJeU1UQTNOamN6TXpJMk5qVTJNREU1TWpNM05UQTBPVFE1TURZMk1UQXdNalk1TVRRek9USTBPVGd6TURNMU1ERTFOemMxTnpNNE9UWTNOelkyT0Rrd056QXdNVFEwTnpVek5UTXlORGcyTURNM056Z3pOekExT1RJME1ESXlNRFk1TlRJNE9UTTBOVEF4TnpVMk9Ea3hOamN5TXpNek56UXlNakEyTWpjME9ETTVOelU1TXpnMk16QXpNamcyTmprM01qVTVNVEl4TkRJNU5qazVNekE0TURjNU9EYzVPRFl4TmpFME16YzVOakkyTURFek16RTNORE0xTVRBd01EQTNORE16TlRBeE9EWXpOakUyTlRnM05qYzBNVE14TWpJMU16QTFNamN3TURVMU5UazFNamMyT0RreU1UWXpNemMzT0RZM09EazBNRFF5TURFM016QTVNRE14T1Rnd09EQTVNREl5T0RRek5EUTJPVGszTkRFeE16VXhORGcxTkRNeU16azBNVEEwTXpNNE1USXpNelUyT0RVNU16STBNakkwT1RZek1ESTFNemM0TlRRMU9ESXlPVGd3TURjeU5EWTFPRFUxT1Rjd09UYzVNVFkyT1Rnd01UWTVNalUyTkRFMU1UQTNNRFV6TXlKZExDQmJJbTFoYzNSbGNsOXpaV055WlhRaUxDQWlNVFU0TmprNU9UUXhNRE15T1RVek1UWTBPRGN3TnpjMk56WXdNVFl5TlRZeE9EVTVOemMwT1RNMk5USTVOVFl5TmpreE5qTXpORFUzTWpjNU5qTXhNak0xTkRBMk5qZzFPVFkxTkRJeE1ETTFOakUzTWpreU1qTXhOalExTkRVM05USXpNemcwTXpZNE1USXlPVFF5TURZeE9ESTFNVFl4TmpFMk9UVTROVEExTnpNNE1USTBPVGc0TlRFd09EQTJOemc0T0RRMk5UTXlNREF5TURJeE56STROREU1TXpJM09Ea3hOamcxTVRjeU1UazBPRFF6TlRRNE5ESXpOVE0xTWpZMk1EazNNRGN5TXpJNU56RXdOak01T1RFeU56QTBNalV6TmpBeU9EWXhNelkwTnpnd016WXpOVFEyTkRFeU1URXlPRGsyT1RneU1UZ3dORFl4TlRBd05UYzJPRFF3TWpNMk5qWTRORE14TWpZNE5qazNPRFUzT1RnNU1Ua3pOalExTXpZME1qZzVNekV6T1RBeU16RTVOVGs0TlRRNU1UUXhOREl3TkRNMk16TTFORFF4T0Rrd09EazNPRGM1TlRRek56UTNOek14TmpjNU16UTFOVFE1T1RBd05qZzNNakl6TmpjeE1EQXdNek14TnpZeE56QXdNamd6TURVNU1qUXpNVGczTXpVek1EVTVOalkzTmpZMk9EQXdOVE15TlRZNE5UUXlNVGcwTnpNM05UVTROalExTVRJNU56VTFPVGcxTlRnMU5EUTRNRE0zTkRNd05EYzBNamMxT0RneE1Ea3pNekkyTkRRek1EazJPRFkzT0RVeE56QXhPRFkwTlRnMU1UWTFNalU1T0RRME5qSXhNREV5TmpNeE16WTJOVE0zTXpVMk1URTFOemM0TWpBNU5qQTBORGswTnpZME9EQXlNakEzTVRnMk5UTTVORGM0TWpRNE9UazBOVEEwTWpFek1ERTJNalE0TWpRNU5qRXdNemcyTmpnMU1qUXlPREF4TmpVNE1USTNOamsyT1RJeE16STNNakU0TVRjek56QXpOemsxTkRNeE1URTFPVFEwT1RVeU5UY3pNRGt5TnpreU5ERTBOVGMwT1RBM056QTVNakl5TkRrMU1EZzBNVFF3TWpJNU56YzJNVEUwTnpJd05ERXdOall5TWpVME9ESTFOemszTnpZMU9ERTFOelU1TmpJNE5ERTVNeUpkWFgwc0lDSnViMjVqWlNJNklDSTNNRGs0T1RZME9EVXdPREUyTmpZeU16WXpNalkzTWpnaWZRPT0ifX1dLCAiY29tbWVudCI6ICJvb2IgaXNzdWFuY2UgdGVzdCJ9fX1dLCAiaGFuZHNoYWtlX3Byb3RvY29scyI6IFsiaHR0cHM6Ly9kaWRjb21tLm9yZy9jb25uZWN0aW9ucy8xLjAiXSwgImxhYmVsIjogIkFyaWVzIENsb3VkIEFnZW50IiwgInNlcnZpY2VzIjogW3siaWQiOiAiI2lubGluZSIsICJ0eXBlIjogImRpZC1jb21tdW5pY2F0aW9uIiwgInJlY2lwaWVudEtleXMiOiBbImRpZDprZXk6ejZNa2Z6aUFpWW9SWkUzV3d3TEI2aVBObVY0ZkhjNmJvUXJnc01ZVXFERGl6aGIzIl0sICJzZXJ2aWNlRW5kcG9pbnQiOiAiaHR0cDovLzEyNy4wLjAuMTo4MDAwIn1dfQ==" // ktlint-disable max-line-length
        val invitation = OutOfBandInvitation.fromUrl(url)
        assertEquals(invitation.requests?.size, 1)
    }
}
