# NFC-Android-Ticket-Application
 A multi-ride amusement park ticket application on NFC memory card. The security features cover mitigating: Man in the Middle attack, Rollback  attack, tearing protection, Write Protection.

# Overview
We are designing a multi-ride amusement park ticket application on NFC memory card. The ticket will 
have a fixed number of rides with validity that will start from the first ride till the next 1 day. To renew 
the tickets, the ticket holder will need to go to the ticket vendor for renewal. The ticket holder can add 
more tickets on top of the existing tickets. We are implementing security features for the NXP MIFARE 
Ultralight C Smart Card. The security features cover mitigating: Man in the Middle attack, Rollback 
attack, tearing protection, Write Protection.

# Features 
- Ticket is first authenticated and registered with 5 tickets.
- The expiry time is set to 1 day after the ticket is used for the first time.
- Each time customer uses the ticket, the ride count is increased by 1.
- The ticket is no longer valid after maximum ride counts have been reached. 
- Customer can buy additional tickets.

# Code Structure
The application logic for ticket and security features can be found in:  NFC-Android-Ticket-Application/app/src/main/java/com/ticketapp/auth/ticket/Ticket.java

# Ticket Application Structure

Page 32: This is the MAC calculated from Page 34 to 39
Page 33: This is the MAC calculated from Page 35 to 39.
Page 34: This page contains the time of expiry. 
Page 35: This specifies the version of application. If there are multiple versions.
Page 36: This specifies the tag of the application, that will be used by reader to identify the specific 
application. 
Page 37: This page contains the time when ticket was issued. 
Page 38: This page contains the number of issued tickets till now. 
Page 41: 16-bit counter to count the tickets used. Every time the ticket is issued, the counter is 
incremented by 1. We compare the incremented counter value with initial counter value and number of 
rides that is in the User data.
Page 42 (AUTH0): 04h: This specifies from where the authentication part starts. In our case, we protect 
the memory from 04h.
Page 43 (AUTH1): Write access to the pages that are specified in AUTH0. 
Page 44 to 46: This memory page contains the authentication key. The authentication key contains the 
hash of the UID with some secret message that is only known by the reader. The reader will 
authenticate the ticket card using this Hash.


# Security Protocols
• **MitM attack:** MitM attack is mitigated by using the MAC scheme. We generate a MAC using a diversified key 
for all the information stored in the card – application tag, version number, expiry time, initial 
counter value, number of rides. 

• **Rollback attack:** 16-bit counter is used to protect against the rollback attack. The counter of the card is a oneway monotonic counter which cannot be rolled back. 

• **Tearing protection:** We keep a backup MAC to cater for the tearing attack. We write the first mac, when issuing 
tickets and the other MAC when the card is being used for the first time. When card is used for 
the first time, there are two write operations – updating counter value, writing expiry time. We 
generate a backup MAC using expiry time.

• **Key diversification:** K = h(master secret | UID) Diversified keys are used for authentication and MAC scheme using 
the UID specific to each card and a master secret. Compromise of one card will not hamper or 
impact security of other cards, because the key for every card is different. 

• **Write protection:** AUTH0 and AUTH1 are set for blocking writing to the card without authentication. Without the 
secret key, the attacker can only know the UID of the card and cannot break the card. 

• **Application Tag and Version:** We use application tag to ensure that, the card is used for the purpose it is designed for. We use 
application version, for backward compatibility. The version number field will be useful later 
when we update the application or need multiple version of the ticket
