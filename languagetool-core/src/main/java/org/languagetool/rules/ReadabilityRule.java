/* LanguageTool, a natural language style checker 
 * Copyright (C) 2005 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package org.languagetool.rules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.Language;
import org.languagetool.LinguServices;
import org.languagetool.UserConfig;
import org.languagetool.rules.Category;
import org.languagetool.rules.CategoryId;
import org.languagetool.rules.ITSIssueType;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.TextLevelRule;
import org.languagetool.rules.Category.Location;

/**
 * A rule that checks the readability of English text (using the Flesch-Reading-Ease Formula)
 * @author Fred Kruse
 * @since 4.4
 */
public class ReadabilityRule extends TextLevelRule {

  private static final int MARK_DISTANCE = 10;
  private static final int MIN_WORDS = 10;
  private static final Pattern NON_WORD_REGEX = Pattern.compile("[.?!…:;,~’'\"„“”»«‚‘›‹()\\[\\]\\-–—*×∗·+÷/=]");

  private final LinguServices linguServices;
  private final Language lang;
  private int level;
  private boolean isToEasy;

  public ReadabilityRule(ResourceBundle messages, Language lang, UserConfig userConfig, boolean isToEasy) {
    this (messages, lang, userConfig, isToEasy, -1, false);
  }
  
  public ReadabilityRule(ResourceBundle messages, Language lang, UserConfig userConfig, boolean isToEasy, int level) {
    this (messages, lang, userConfig, isToEasy, level, false);
  }
  
  public ReadabilityRule(ResourceBundle messages, Language lang, UserConfig userConfig, boolean isToEasy, boolean defaultOn) {
    this (messages, lang, userConfig, isToEasy, -1, defaultOn);
  }
  
  public ReadabilityRule(ResourceBundle messages, Language lang, UserConfig userConfig, 
      boolean isToEasy, int level, boolean defaultOn) {
    super(messages);
    super.setCategory(new Category(new CategoryId("TEXT_ANALYSIS"), "Text Analysis", Location.INTERNAL, false));
    setLocQualityIssueType(ITSIssueType.Style);
    if(!defaultOn) {
      setDefaultOff();
    }
    this.lang = lang;
    this.isToEasy = isToEasy;
    if (userConfig != null) {
      linguServices = userConfig.getLinguServices();
      if(level >= 0) {
        this.level = level;
      } else {
        int lv = userConfig.getConfigValueByID(getId());
        if(lv >= 0) {
          this.level = lv;
        } else {
          this.level = isToEasy ? 4 : 2;
        }
      }
    }
    else {
      linguServices = null;
    }
  }
  
  @Override
  public String getId() {
    if(isToEasy) {
      return "READABILITY_RULE_SIMPLE";
    } else {
      return "READABILITY_RULE_DIFFICULT";
    }
  }

  @Override
  public String getDescription() {
    if(isToEasy) {
      return "Readability: To easy text";
    } else {
      return "Readability: To difficult text";
    }
  }

  @Override
  public int getDefaultValue() {
    return (isToEasy ? 4 : 2);
  }
  
  @Override
  public boolean hasConfigurableValue() {
    return true;
  }

  @Override
  public int getMinConfigurableValue() {
    return 0;
  }

  @Override
  public int getMaxConfigurableValue() {
    return 6;
  }
  
  @Override
  public String getConfigureText() {
    return "Level of readability 0 (very difficult) to 6 (very easy):";
  }
  
  private static String printMessageLevel(int level) {
    String sLevel = null;
    if (level == 0) {
      sLevel = "Very difficult";
    } else if (level == 1) {
      sLevel = "Difficult";
    } else if (level == 2) {
      sLevel = "Fairly difficult";
    } else if (level == 3) {
      sLevel = "Medium";
    } else if (level == 4) {
      sLevel = "Fairly easy";
    } else if (level == 5) {
      sLevel = "Easy";
    } else if (level == 6) {
      sLevel = "Very easy";
    }
    if (sLevel != null) {
      return " {Level " + level + ": " + sLevel + "}";
    }
    return "";
  }
  
  protected String getMessage(int level, int FRE, int ASL, int ASW) {
    String simple;
    String few;

    if(isToEasy) {
      simple = "simple";
      few = "few";
    } else {
      simple = "difficult";
      few = "many";
    }
    return "Readability: The text of this paragraph is to " + simple + printMessageLevel(level) + ". To "
        + few + " words per sentence and to " + few + " syllables per word.";
  }
  
  /**
   * get level of readability (0 - 6)
   */
  private int getReadabilityLevel(double FRE) {
    if (FRE < 30) {
      return 0;
    } else if (FRE < 50) {
      return 1;
    } else if (FRE < 60) { 
      return 2;
    } else if (FRE < 70) {
      return 3;
    } else if (FRE < 80) {
      return 4;
    } else if (FRE < 90) {
      return 5;
    } else {
      return 6;
    }
  }

  /**
   * get Flesch-Reading-Ease (Formula for readability) for English
   * the formula dependence on the language and has to be overridden for every supported language
   */
  protected double getFleschReadingEase(double ASL, double ASW) {
    return 206.835 - ( 1.015 * ASL ) - ( 84.6 * ASW );
  }
  
  private static boolean isVowel(char c) {
    if(c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u' || c == 'y' ||
        c == 'A' || c == 'E' || c == 'I' || c == 'O' || c == 'U' || c == 'Y') {
      return true;
    }
    return false;
  }
  
  /**
   * A simple method to count the Syllables of a word
   * TODO: further improvement of the method
   * A hyphenation service should be used if available (e.g. from LO extension)
   * Has to be overridden for every language
   */
  protected int simpleSyllablesCount(String word) {
    if(word.length() == 0) {
      return 0;
    }
    if(word.length() == 1) {
      return 1;
    }
    int nSyllables = 0;
    boolean lastDouble = false;
    for (int i = 0; i < word.length() - 1; i++) {
      char c = word.charAt(i);
      if(isVowel(c)) {
        char cn = word.charAt(i + 1);
        if(lastDouble) {
          nSyllables++;
          lastDouble = false;
        } else if(((c == 'e' || c == 'E') && (cn == 'a' || cn == 'o' || cn == 'e' || cn == 'i' || cn == 'y')) || 
            ((c == 'a' || c == 'A') && (cn == 'e' || cn == 'i' || cn == 'u')) ||
            ((c == 'o' || c == 'O') && (cn == 'o' || cn == 'i' || cn == 'u' || cn == 'a')) ||
            ((c == 'u' || c == 'U') && (cn == 'i' || cn == 'a')) ||
            ((c == 'i' || c == 'I') && (cn == 'e'|| cn == 'o'))) {
          lastDouble = true;
        } else {
          nSyllables++;
          lastDouble = false;
        }
      } else {
        lastDouble = false;
      }
    }
    char c = word.charAt(word.length() - 1);
    char cl = word.charAt(word.length() - 2);
    if (cl == 'e' && (c == 's' || c == 'd') || cl == 'u' && c == 'e') {
        nSyllables--;
    } else if (isVowel(c) && c != 'e') {
        nSyllables++;
    }
    return nSyllables <= 0 ? 1 : nSyllables;
  }

  @Override
  public RuleMatch[] match(List<AnalyzedSentence> sentences) throws IOException {
    List<RuleMatch> ruleMatches = new ArrayList<>();
    String msg;
    int nSentences = 0;
    int nWords = 0;
    int nSyllables = 0;
    int pos = 0;
    int startPos = 0;
    int endPos = 0;
    for (int n = 0; n < sentences.size(); n++) {
      AnalyzedSentence sentence = sentences.get(n);
      AnalyzedTokenReadings[] tokens = sentence.getTokensWithoutWhitespace();
      if(nSentences == 0) {
        startPos = pos + tokens[1].getStartPos();
      }
      nSentences++;
      for (AnalyzedTokenReadings token : tokens) {
        String sToken = token.getToken();
        if (!token.isWhitespace() && !NON_WORD_REGEX.matcher(sToken).matches()) {
          nWords++;
          if(linguServices == null) {
            nSyllables += simpleSyllablesCount(sToken);
          } else {
            nSyllables += linguServices.getNumberOfSyllables(sToken, lang.getDefaultLanguageVariant());
          }
        }
      }
      if(sentence.hasParagraphEndMark(lang) || n == sentences.size() - 1) {
        if (nWords >= MIN_WORDS) {
          endPos = pos + tokens[tokens.length - 1].getEndPos();
          /* Equation for readability
           * FRE = Flesch-Reading-Ease
           * ASL = Average Sentence Length
           * ASW = Average Number of Syllables per Word
           * English: FRE = 206,835 - ( 1,015 * ASL ) - ( 84,6 * ASW )
           * German: FRE = 180 - ASL - ( 58,5 * ASW )
           */
          double ASL = (double) nWords / (double) nSentences;
          double ASW = (double) nSyllables / (double) nWords;
          double FRE = getFleschReadingEase(ASL, ASW);
          int rLevel = getReadabilityLevel(FRE);

          endPos = pos + sentence.getText().length();
          
          if (isToEasy && rLevel > level) {
            msg = getMessage(rLevel, (int) FRE, (int) ASL, (int) ASW);
            RuleMatch ruleMatch = new RuleMatch(this, startPos, startPos + MARK_DISTANCE, msg);
            ruleMatches.add(ruleMatch);
            msg = getMessage(rLevel, (int) FRE, (int) ASL, (int) ASW);
            ruleMatch = new RuleMatch(this, endPos - MARK_DISTANCE, endPos, msg);
            ruleMatches.add(ruleMatch);

          } else if (!isToEasy && rLevel < level) {
            msg = getMessage(rLevel, (int) FRE, (int) ASL, (int) ASW);
            RuleMatch ruleMatch = new RuleMatch(this, startPos, startPos + MARK_DISTANCE, msg);
            ruleMatches.add(ruleMatch);
            msg = getMessage(rLevel, (int) FRE, (int) ASL, (int) ASW);
            ruleMatch = new RuleMatch(this, endPos - MARK_DISTANCE, endPos, msg);
            ruleMatches.add(ruleMatch);

          }
        }
        nSentences = 0;
        nWords = 0;
        nSyllables = 0;
      }
      pos += sentence.getText().length();
    }
    return toRuleMatchArray(ruleMatches);
  }
 
}