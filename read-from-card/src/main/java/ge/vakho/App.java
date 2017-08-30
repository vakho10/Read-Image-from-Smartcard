package ge.vakho;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;
import javax.smartcardio.CardTerminals.State;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jmrtd.BACKey;
import org.jmrtd.PassportService;
import org.jmrtd.lds.icao.DG2File;
import org.jmrtd.lds.iso19794.FaceImageInfo;
import org.jmrtd.lds.iso19794.FaceInfo;

import net.sf.scuba.smartcards.CardService;

public class App
{
    public static void main(String[] args) throws Exception
    {
        Security.addProvider(new BouncyCastleProvider());

        TerminalFactory factory = TerminalFactory.getDefault();
        List<CardTerminal> terminals = factory.terminals().list(State.CARD_PRESENT);
        if (terminals.size() == 0)
        {
            throw new RuntimeException("There is no terminal!");
        }
        CardTerminal terminal = terminals.get(0);
        if (!terminal.isCardPresent())
        {
            throw new RuntimeException("There is no card present in this terminal!");
        }

        PassportService service = new PassportService(CardService.getInstance(terminal));
        service.open();

        String cardNumber = "11IC76084";
        String dateOfExpiry = "220425";
        String dateOfBirth = "930514";

        service.sendSelectApplet(false);
        service.doBAC(new BACKey(cardNumber, new Date(1993, 4, 14), new Date(2022, 3, 25)));

        try (InputStream dg2Stream = service.getInputStream(PassportService.EF_DG2))
        {
            DG2File dg2 = new DG2File(dg2Stream);
            List<FaceImageInfo> allFaceImageInfos = new ArrayList<FaceImageInfo>();
            List<FaceInfo> faceInfos = dg2.getFaceInfos();
            for (FaceInfo faceInfo : faceInfos)
            {
                allFaceImageInfos.addAll(faceInfo.getFaceImageInfos());
            }

            InputStream imageInputStream = allFaceImageInfos.get(0).getImageInputStream();
            DataInputStream dataInputStream = new DataInputStream(imageInputStream);
            byte[] imageBytes = new byte[allFaceImageInfos.get(0).getImageLength()];
            dataInputStream.readFully(imageBytes);

            FileOutputStream out = new FileOutputStream("img.jpg");
            out.write(imageBytes);

        }

    }
}
