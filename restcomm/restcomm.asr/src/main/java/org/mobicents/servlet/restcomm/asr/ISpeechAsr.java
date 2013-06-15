/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.restcomm.asr;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;

import com.iSpeech.SpeechResult;
import com.iSpeech.iSpeechRecognizer;
import static com.iSpeech.iSpeechRecognizer.*;
import com.iSpeech.iSpeechRecognizer.SpeechRecognizerEvent;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.Configuration;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class ISpeechAsr extends UntypedActor implements SpeechRecognizerEvent {
  private static final Map<String, String> languages = new HashMap<String, String>();
  static {
    languages.put("en", "en-US");
    languages.put("en-gb", "en-GB");
    languages.put("es", "es-ES");
    languages.put("it", "it-IT");
    languages.put("fr", "fr-FR");
    languages.put("pl", "pl-PL");
    languages.put("pt", "pt-PT");
  }
  
  private final String key;
  private final boolean production;

  public ISpeechAsr(final Configuration configuration) {
    super();
    key = configuration.getString("api-key");
    production = configuration.getBoolean("api-key[@production]");
  }
  
  private AsrInfo info() {
    return new AsrInfo(languages.keySet());
  }

  @Override public void onReceive(final Object message) throws Exception {
    final Class<?> klass = message.getClass();
    final ActorRef self = self();
    final ActorRef sender = sender();
    if(AsrRequest.class.equals(klass)) {
      try {
        sender.tell(new AsrResponse<String>(recognize(message)), self);
      } catch(final Exception exception) {
        sender.tell(new AsrResponse<String>(exception), self);
      }
    } else if(GetAsrInfo.class.equals(klass)) {
      sender.tell(new AsrResponse<AsrInfo>(info()), self);
    }
  }
  
  private String recognize(final Object message) throws Exception {
    final AsrRequest request = (AsrRequest)message;
    final File file = request.file();
    final String language = languages.get(request.language());
    final iSpeechRecognizer recognizer = iSpeechRecognizer.getInstance(key, production);
	recognizer.setFreeForm(FREEFORM_DICTATION);
    recognizer.setLanguage(language);
    final SpeechResult results = recognizer.startFileRecognize("audio/x-wav", file, this);
    return results.Text;
  }
  
  @Override public void stateChanged(final int event, final int value,
      final Exception exception) {
    if(SpeechRecognizerEvent.RECORDING_ERROR == event) {
      // We don't use the recorder.
    }
  }
}