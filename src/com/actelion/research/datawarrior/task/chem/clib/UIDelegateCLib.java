/*
 * Copyright 2017 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91, CH-4123 Allschwil, Switzerland
 *
 * This file is part of DataWarrior.
 *
 * DataWarrior is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * DataWarrior is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with DataWarrior.
 * If not, see http://www.gnu.org/licenses/.
 *
 * @author Thomas Sander
 */

package com.actelion.research.datawarrior.task.chem.clib;

import com.actelion.research.chem.Canonizer;
import com.actelion.research.chem.MoleculeFilter;
import com.actelion.research.chem.StereoMolecule;
import com.actelion.research.chem.SubstructureFilter;
import com.actelion.research.chem.io.CompoundFileHelper;
import com.actelion.research.chem.io.RDFileParser;
import com.actelion.research.chem.io.RXNFileParser;
import com.actelion.research.chem.reaction.Reaction;
import com.actelion.research.chem.reaction.ReactionEncoder;
import com.actelion.research.datawarrior.DEFrame;
import com.actelion.research.datawarrior.task.AbstractTask;
import com.actelion.research.datawarrior.task.TaskUIDelegate;
import com.actelion.research.gui.*;
import com.actelion.research.gui.clipboard.ClipboardHandler;
import com.actelion.research.gui.editor.EditorEvent;
import com.actelion.research.gui.editor.GenericEditorArea;
import com.actelion.research.gui.editor.SwingEditorPanel;
import com.actelion.research.gui.generic.GenericEventListener;
import com.actelion.research.gui.hidpi.HiDPIHelper;
import com.actelion.research.table.model.CompoundTableModel;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

public class UIDelegateCLib implements ActionListener,ChangeListener, GenericEventListener<EditorEvent>,ItemListener,TaskConstantsCLib,TaskUIDelegate {
	private static final int EDITOR_HEIGHT = 360;

	private static final String COMMAND_RETRIEVE = "retrieve";
	private static final String COMMAND_OPEN_REACTION = "Open Reaction...";
	private static final String COMMAND_SAVE_REACTION = "Save Reaction...";
	private static final String ITEM_CUSTOM_REACTION = "<Custom Reaction>";
	private static final String[][] REACTION = {
			{ "1,2,4-Triazole from acetohydrazide", "gJU@DPdru@XIFTs` eM`AIxLH@!gO|@ABeKNLuRA@#s@gop Ss` s}uKHl#!REpI~@A\\}bOrckDH !RJCt@PzMRO@ !RTv\\`iTqg_g}hSJT_hqj\\##" },
			{ "1,2,4-Triazole from carboxylic acid", "gC``@dfZ@pb@ eM`AIxLH@!gO|@ABeKNLuRA@#s@`l Ss` s@EKHl#!RY?r~@F_]hrJB !RJMt@@zMJo@ !RTv]@IPqgog|hCBT_hwj\\##" },
			{ "1,2,4-Triazole from carboxylic ester", "gNp`@df^Zj@rd^t_d\\hqca`\u007FuU` eM`AIxLH@!gO|@ABeKNLuRA@#s@`l@@ Ss` s@EKHl#!RY?r~@F_]rLI~pLb]hvHb !RJMt@@zOJo@ !RTv]@IPqgog|hCBT_hwj\\##" },
			{ "Benzimidazol with acid or ester", "gGP`@dfYj`Mi_PcFVFC?TZ die@@@iJ[gxZB@@CA@jIDcApLcAp\\cApBcAp@!dmM@@@iJYe_JEYB`b@CDbQ`xNQ`xAQ`xEQ`x@#K@hFP@ Kgw[XCj`h KFuWZss@nt@#!Rog~wW[A]mpJA_zLdw@ !RlGvwO[y?lCrwO[A}lGvpOJObw@ !RXFicFKx@bCtlFE~~bOrH?BU`bCvcVdH##" },
			{ "Benzimidazol with aldehyde", "eMHAIXLJ@ die@@@iJ[gxZB@@CA@jIDcApLcAp\\cApBcAp@!dmM@@@iJYe_JEYB`b@CDbQ`xNQ`xAQ`xEQ`x@#K@kH Kgw[XCj`h KFuWZss@nt@#!R?g~HwZNbO@ !RbOvw__y?b@Jw_Xc}bOvH@jOlw@ !RXAiSFOx@bGtlFG?~b@JH@fU`bGvcFlH##" },
			{ "Benzofuran", "eM@H~CB` daDH@@RYe[hB@@LHBLTfIV@!detH@@rQQQHyPsPLA@A@#KfIX s}HmpOP Kzb\\JbVtnP#!R@AL_[JNB?@ !Rb@K~@Hc}b@Jw@h`BbOvc}DH !RXAiSFOx@bOtlFG?~b@JH@ha}h~kB##" },
			{ "Benzothiophene", "eM@H~CB` did@`@BDinWahB@@LDAHprXeX!det@`@BLdTTRNTLtC@P@P#KdIX sy@bw@} KxbTJbVtnP#!R@FMo[JNBo@ !Rb@KW@gx@bGvH@m\\Bb@JH_ZLJP` !RXNhSFGx@bGtlFC}~bOrH?Hb}h?jB##" },
			{ "Benzoxazol with aromatic aldehyde", "eMHAIXLJuL daFH@BAIf]n``@@pPB@!devH@JCIEEDceCM@pD@D#K@kH KdVlNmPL KdUUX]ru@p#!R_g?pWZMTw@ !R|Gu~_?A|m{wp__A}|GqcXmp !RxNUSYpE?b@IlYs}~bKvHoX`Bhyk\\##" },
			{ "Benzoxazol with carboxylic acid", "gC``@dfZ@pb@ daFH@BAIf]n``@@pPBbVHp\\EHp\\CHp\\GHp\\@!devH@JCIEEDceCM@pD@FIDcAp\\cApBcApRcAp@#K@hFP KdVlNmPL KdUUX]ru@p#!R_g~ww_C]hrI\\ !R|Ou~_?A|m{wp?_C}|GqckIp !RxNUSYpE?b@IlYs}~bKvHoX`Bhyk\\##" },
			{ "Benzthiazol", "eMHAIXLJ@ daF@`B@HRYg[hH@@LD@hebLGARLG@rLGArLG@!dev@`J@HrQQQHyPsPLA@AbQHp\\GHp\\@hp\\Dhp\\@#K@kH KdVlNmPL KdUUX]ru@p#!R_g?pWZMTw@ !R|Gu~_?A|m{wp__A}|GqcXmp !RxNUSYpE?b@IlYs}~bKvHoX`Bhyk\\##" },
			{ "Buchwald-Hartwig", "gJX@@eST`[Q^`bJTLAs`D@c\\C?wQD eF@Hp]QQkHcDkOpa`!eM`BN``#SPG@ a` STx#!R?g}r@K\\BbGuc@kp !R_vrcUOp !R@IM?dzOJ_@##" },
			{ "Decarboxylative coupling", "gOp@DjWkB@@L]DXNCYDXeXTXN@ dklB@@Q]RZ{efSZ``X@CADvTEGhaxqFLG@D_zbD!dg|H@@RVYYwySm``@@@@qrLXN@HEFC`uFC`@#KemPAmz KS@F@UJAyp@n KSCERr^_Zkc[t#!RmwvH?_x@?g?~_xc}hws\\ !RbGvH@oy?bGvw?Xc}mpK~_?y??`Bw_Xc}hxRl !RbGw~_{_}bOvw@oy??g?~@K]}bOvw?K_|mwvcZbp##" },
			{ "Diels-Alder", "gCa@@dkHD gC`@Die@`!daDH@@RVU[fjf@H#IVQx qbq IVQKdr@#!R?g~H__y?hzJ\\ !R?`Bw_[\\Bh~pB !R?g~H_X`B?g~H_Xc}?g~czap##" },
			{ "Fischer Indole", "gGP`@TeYi`LHo}`@ daE@@@aJyUnh@@@ppHp!dmN@@@RfYWraV`XH`@`#KAiX`] KfADFurT Kf`TFk{WNb`#!R`W|h@xaB_lAH?VOBW@ !Rm{wp_Wy?|Gsp__A}|Grcbmp !RXNhSFGx@bKtlFC}~bOrH@jU`bKvcAdH##" },
			{ "Friedlaender Chinoline", "gGP`@TeYi`LHo}`@ difH@JAIYgxVB@@CAHh!dcn@@@rQQQQHutz@DA@@D#K@[\\`Y K@jBB|jtX KxbTJ{rhhsM#!R`W|h@xaB_lAH?VMJ{@ !R_hAH@gx@bGwW@h`Bb@JH_ZNbH` !RbOsW?Gx@bGu~@M^}u?rH?Hc|u?rHoZOz``##" },
			{ "Grignard alcohol", "gOP`AddvYhCBRtlYXXO}Hbh gCa@@dsPFJVQFIV[HcDkO}PP!gCa@@duPD#S@@xP aP SGT#!RBDHHOX{r_g~w_S@ChyIb !R_`BH?M_|htjB !R_g|HOS\\}hwk\\##" },
			{ "Grignard carbonyl", "gCa@@dsPFJVQFIV[HcDkO}PP eM`AIxLH@!gCa@@dsPD#aP SGP SGT#!R_`BH?M_|hqJB !RTFL@XzOJO@ !R_g|HOS\\}h{k\\##" },
			{ "Heck non-terminal vinyl", "gJP@DjVhCBP eF@HpXudQbUfxh!gGP@DjYj`H#sWXh` qX sWXhf#!R}pOp@?x@mpJceKp !R_zNc`kp !RM{uHo_x@MxK~_rNj?@##" },
			{ "Heck terminal vinyl", "eM@HvCAH eF@HpXudQbUfxh!gC`@DiZDE@#S_` qH sWXH#!R_yL@dzNTo@ !R_zNc`kp !R_~L@xw?lHzk|##" },
			{ "Heteroaromatic nucleophilic substitution", "gGX`DLdmmRAuH~bbJJL@Q\\xA@HwD\u007F}Hqh dif@@@RVe~F``@@zaJLTdaDUPEPk|HQ}xHpfxP!eM`BN``#SGP@ aH@ STx#!R@nSMyG~AW@`caMNB?@ !RbOrH?Xc|?g~w?Xc}bOw~_zMjO@ !R_qL@DzOzo@##" },
			{ "Huisgen Cu-catalyzed 1,4-subst", "eMHAIXLIuSzhcIBDQbUg~p eM@H~CB`!gO|@ADeKNKURA@#QL sTh s@@Tl`#!R?g~H?VMBk@ !RUxH@HqNdW@ !RSI]LjLCjpNTU@J`sX~k|##" },
			{ "Huisgen disubst-alkyne", "gC`@Diz@` eMHAIXLIuSzhcIBDQbUg~p!daG@@@aDiY\\IjjPB#sThh QL s@@I\\j@#!RvuJHWW}|t|P\\ !R?g~H?VMBk@ !RSJaLe|CepNhU_patjCMczop##" },
			{ "Huisgen Ru-catalyzed 1,5-subst", "eMHAIXLIuSzhcIBDQbUg~p eM@H~CB`!gO|@ADeK^KURA@#QL sTh s@@I\\`#!R?g~H?VMBk@ !RUxH@HqNdW@ !RSJaLe|CepNhU_z`sX~k|##" },
			{ "Imidazol", "gNqDLLRxbRwTu@XP?{@ gJT@@dffhCAAZf[P?zlP!gKT@Adi\\Vf@`#QBD@ sl|@@ slxq@#!RmpK~@K_|bOrH?_x@huRb !R?g~Hw_y?m}vcPep !RTv]`YRqg?g~cZkp##" },
			{ "Indole", "eM@H~CB` daF@@@RYe[hB@@LHBLTfIV@!dev@@@rQQQHyPsPLA@A@#KdIX syHmpOP KxbTJbVtnP#!R@AL_[JNB?@ !Rb@K~@Hc}b@Jw@h`BbOvceDH !RXAiSFOx@bOtlFG?~b@JH@ha}h~kB##" },
			{ "Mitsunobu Imide", "gNx`DJdssTpFDUegEZFApp_zlp gCa@@duPFDEhoRQFJFC?X`!gNx`DJdlkTpFDP#s||Bw@ SCD s||mW`#!R?g~w?Xc}m?s~@K_}hus\\ !R?g|w@x`}h~c\\ !R?g~w?[_|?g?~@Hc}hrj\\##" },
			{ "Mitsunobu Phenol", "gOq@@drm\\@@Aa@ gCa@@duPFDEhoRQFJFC?X`!didH@@RfU~F``@@`#KeAK\\uJ KAt@` KeCjVbsTh#!Rm?w~_{\\Bm?vw?[\\Bhqs\\ !R?g|w@x`}h~c\\ !RUsthO_y?mpHhPk_}mwvw@jOjO@##" },
			{ "Mitsunobu Sulfonamid", "gGXhMD@cIIBmPFDp gCa@@duPFDEhoRQFJFC?X`!dazD`La@BLddLruT@XQ`#sT{q? K@LD@ KRVnFc{@#!RKMBk_{BCc{sPOFO\\o@ !R?g|w@x`}h~c\\ !RWLcskz`}etKH@oy?bOuczgp##" },
			{ "Mitsunobu Tetrazole 1", "gFr@ALTi[FZhCAPuRQT_beV@ gCa@@duPFDEhoRQFJFC?X`!daD`@@aLReUpffj@H#Khth~x SCD KZNgRiq@#!RcM^Lr@Aj@ATU@JLB@` !R?g|w@x`}h~c\\ !RXAiSFOx@KA`XKFU`bOvcflH##" },
			{ "Mitsunobu Tetrazole 2", "gFr@ALTi[FZhCAPuRQT_beV@ gCa@@duPFDEhoRQFJFC?X`!daD`@@aBRegpfji@H#Khth~x SCD KGuiZIq@#!RcM^Lr@Aj@ATU@JLB@` !R?g|w@x`}h~c\\ !R?gAWQU\\z`rQEUtTh_uEczdH##" },
			{ "N-arylation heterocycles", "gC`h@iPIMTAuM@ dmW`@@aJFURjU_jjZ`Cj@oPEqclxA@HjpJIUJyPjeWxPEy@WjpfES\\Lj!eF`BLD#aD QP@@@ QX#!Rm?rw?_x@hxc| !RJKuWo_x`?n|@?e^BJHIWgU^}UzJcoKp !R@ANcJkp##" },
			{ "Negishi", "gC`@H}PFJa\\@axABrHqJrjCdk\u007FY@ gC`@H}PFJa\\@axABrHqJrjCdk\u007FY@!gC`@Dij@uTTe@#sL@` SpC sOHT#!R_`CW_Xa}hvK| !R_`CW_Xa}hpk| !RO|L@ps~\\h{j|##" },
			{ "Niementowski quinazoline", "gNu@DQdTTKUHFDUioQQEZFAipBG~gFl deVD@FADfygFV``@@pQF@!dmMH@DxDfVUzzUZ`PH@H#sBu@@@ KadCY{Dl}p KaeKZsvIY{`#!RatJ~dH\\]T`BGPlE`XvSB !Rog~wO[@}og~pOK\\}lGvp_[@|hqKl !Rog~w_[A}og~~_{@|lKvpo[@}lCrcVnp##" },
			{ "Nucleophilic arom subst ortho to nitro", "gGX`DLdmmRAuH~bbJJL@Q\\xA@HwD\u007F}Hqh deVDAHAHeNR[fTYZ@`@CFNRPb@!dmuDAHXDbTyInYTUZBB@@`#KAm@@@ KFu\\NRjCIP KFu\\NRQWNr`#!RW`AwHhF@Mwtw_FM|h` !Rm{vw_Gy?m{wpoWy?mwq~_?C}hwKl !Rm?vwoKy?M?wpo]^Bog~~_}_]|Kvcnfp##" },
			{ "Nucleophilic arom subst para to nitro", "gGX`DLdmmRAuH~bbJJL@Q\\xA@HwD\u007F}Hqh deVDAHAHeNR[e_aZ@B@CFIRPb@!dmuDAHdDbTyInUwaZ@B`@`#sAt@@ KrVlNRFoX@ KrVlNRFoZF`#!RPP@OhdF@MwtwOJNBH` !RmsvwoKy?msvpO[y?og~poWy?hpkL !RmwvwwCy?MwvpOSy?Og~pO[y?|Kvcndp##" },
			{ "Oxadiazole", "gC``@dfZ@pb@ eM`AIxLH@!gOu@HPeKNMKTA@#sXpp siX sZcTL`#!R_g~w?_C}hzK\\ !R`FL@XzMJO@ !RcI^Lr@Bj@NWe@LcShwk|##" },
			{ "Paal-Knorr", "gGR@AMTinZ`Mh_R[EKN@PBBcAp_{PlZ daxD@@YIgYjf@OdRB`!gOx@@eLvmLtA@#aP sA{|tCP saGO[|#!R]{qpoGx@]?uw`jLb@` !RbKvH@gx?_`CW@m^}_`Bcj`H !RcLbLepCeyP@@jnT?h?i|##" },
			{ "Phtalazinone", "gCd@ADki@pPdYlxA@H\u007FvX daxL@@QdfufbX@paKQtXzFC\u007FXx!daEH@DHDfYVyji`B#KrN@@ sT`m?@@ KRSMQp[t#!R@Jp?kK?chvI| !RbOq~@Hb}_c?Wo]_|_c~cJcp !R_g~wo_A}_g~w_Gy?|Gvc^ap##" },
			{ "Pictet-Spengler", "eMHAIXLJ@ dif@@@RUe^Fh@@@pPB@!dmN@@@rQQJEJfeZj@B@B#K@cX K`n[^Ckdh K`a_Ms[TY\\`#!R_vq?DzO|o@ !R?`BH@hc}?g~H@hc}bOvH@jL\\`` !RbOw~@H`B?g~H@ox@bOvH@h`BbOvcfdH##" },
			{ "Piperidine indole", "gOy@FDfUkZj`LBlZ diV@@@RfU|kahDB@C@`Jf@!foQP@@@ZRe]fUrTejsjiiB@h@B#K@kY?w^ K{V[Rcae@ K__y}jFv]X]JcH#!R?`BH@k\\B?`C~@K\\BhxKb !RXAiSFOx@bOtlFH`Bb@JH?ZNLP` !RpIdknqh_Fa|knqh_XFUSY?y?b@HlYxc}bOvH@jNZm@##" },
			{ "Pyrazole", "eMPBchLJmOhbLpp_{H dedB@@sirISQPluL`FtMzBbLMCA?n@t`!gKT@ADi\\Yi@`#S\\@ q@Hn@@ saRr`#!R@Jq?kJNt_@ !R_`CW?Gx@uwwW?Gx?_`CW_Wx?hxKB !RGqk~_uMgGvVczkp##" },
			{ "Reductive amination", "gCh@AGj@vb}ADXhXMjSO?UD gCa@@dsPFtTXhXO}b@!gCh@@eMPD#sp@L SCD sqS`#!R?`BwwXc]h?q| !R?g~w?Xc}h~Q\\ !R@Nq?{@CcH~jB##" },
			{ "Schotten-Baumann", "gGX@@eRuJAu@~`bIbL@QdxA@ypB@Q\\xA@Hw@\u007F}Lqh gC``@dfZ@pb@!gJY@DDfVhB#sHE@@ soP` soRaP#!R_K]?_kspWU}cIzOBO@ !R_g~w__A}hpk\\ !R_g|woPb}Mwrcnmp##" },
			{ "Sonogashira", "eM@H~CB` eF@HpXtfIV[b`!gC`@Diz@`#Qv qH qra#!ROwx@_cLb_@ !R@FLsbkp !ROwx@_c}~L{i|##" },
			{ "Spiro-chromanone", "gOq@@drm[UTA`UcXqhp\\@ deTD@@YIfyqehH@@LDq`!f`i@`@@\\RYeeo]gIF]yhJBbjH@LX^Q`x@#kItMm]C} KVND~h@wMP kUE`eyPtIYqMmi]C}#!Rm?w~_{\\Bm?vw_[\\Bhus\\ !R?g~H?[]}?g~H@k]}bOvH?X`Bh~Rl !R?g~H_[]}?g~H@oy?mwvH_Xa}?g~w@h`Bmwvw_[\\Bhzil##" },
			{ "Stille", "eF@Hp]QQkHcDk@ eF@HpXtfIV[b`!eF@HpP#QP a` Qd#!R_zNc`kp !R_vrcC@H !R_~pcj`H##" },
			{ "Sulfonamide", "gJPdEaDPHRZe`MhXphp\\G~p` gGX@@eRuJAu@~`bIbL@QdxA@ypB@Q\\xA@Hw@\u007F}Lqh!gJXhMD@cIHUhB#sBEl@ sHM@@ sa[KP#!R_g}~_?A}mwvcbip !R|Grw_Gy?|Ouw?ZOBo@ !R_`BH_]]}u?rc^op##" },
			{ "Suzuki", "gC`h@iPIMTAwM@ eF@HpXudQbUfx`!eF@HpP#aD QP QX#!R?g~H?[_}hws\\ !R_vrctop !R@ANczkp##" },
			{ "Tetrazole regioisomere 1", "eM`AIxLH@ eMHAIXNjXsdQbUg~p!gOr@ADTiYqZjPH#Ss` qA@ s@@WB`#!RJCt@PzNRO@ !R_g?p?ZNJw@ !RcI^Lr@Bj@NWe@LcShwk|##" },
			{ "Tetrazole regioisomere 2", "eM`AIxLH@ eMHAIXNjXsdQbUg~p!gOr@ACTjVMZf`H#Ss` qA@ s@EHL`#!RJCt@PzNRO@ !R_g?p?ZNJw@ !RDqa`zfT_KAa~@A`Sh{hB##" },
			{ "Thiazole", "gGY@BDfYj`LeGS~bnLNQFIV@ gChHD@aIf`LDD!daF@`L@HrQQPyULu@D#sBw`O KJiFP KJijLu{@#!R|Ou~_?A|m{wp?VNlg@ !RoPc\\ZG|AhxIB !RDqa`zbpXOwy~@Hb}b@JcfdH##" },
			{ "Thiourea", "gGY@HDfViPNeGOiHcdc@DGN@PBKg@HAFyG\u007FiBM@ gChHD@aIU`H!gGTHE`DILkU@P#sB`t@ sXxT sXzcU#!ROk|w_WB]Msp~_zMBw@ !R@DOpO_@Bhrh| !R_g|HoS^}BGpw_JOzw@##" },
			{ "Triaryl-imidazole", "f`q@`@@LRYfWg^Qg^ZB`@@@@LHLkBb@ eMHAIXLJuL!fdeP@@@TrQQQQEJKKDiipXsMA@@A@@D#KAMX}gAb?zm^WjP k@C` k@ECpe@`iYUIyuMQm}]#!RROqHw_x`rKp~os\\BR@HHOP`}bOw~@Cz?b@JH@ox@hvrB !Rm?w~_zNBS@ !RXFUSYq`l?g|lYw?~LXCPsQaSFBpXKMCRL[?PsQaSFELqozNZO@##" },
			{ "Urea", "gGY@HDfViPNeGOiHcdc@DGN@PBKg@HAFyG\u007FiBM@ gCi@DDeV@`!gGU@E`drmTA@#sB`t@ s\\xT s\\zcU#!ROk|w_WB]Msp~_zMBw@ !R@DOpO_@Bhrh| !R_g|HoS^}BGpw_JOzw@##" },
			{ "Williamson Ether", "gC``@dfZ@pbkDk\u007FX@ eM@HzCBHudQbU`!gJQ@@eKU@ZqJ@#sB|` sHE srraP#!R_g}w_WA}hrK\\ !R@LL?pzLJ_@ !R@Np?{@BcOzNcngp##" },
			{ "Wittig", "gNq`Afdm[MHFtlYXXO}HeX gGQ@@eMuTAaMEELRlnQFIV_{`b!gC`@Dij@`#S@X`@ qAF@ sWHH#!R]JreTGx@bGwW_ZaBhtkB !R?`BH_]\\BbOsWOZNB@` !RO~L@xs~lhwj|##" },
//		{"Passerini", "gC``@dfZ@pb@ eM`PfzO`` eMHAIXLJ@!defL@DBaRY[yjjZLHB#IXUx Q^ Idj IWDJIhVx#!Rm?rw?_x@hvHB !R_?x@?jMx_@ !R_vq?DzMl@` !R?`Bw@h`BmwvH__x@b@K~@Ha}h}kB##"},
		};

	private Component	mParent;
	private CompoundTableModel mTableModel;
	private SwingEditorPanel	mDrawPanel;
	private JPanel		mReactantPanel;
	private JComboBox	mComboBoxMode,mComboBoxReaction;
	private ArrayList<CompoundCollectionPane<String[]>> mReactantPaneList;
	private Reaction	mCustomReaction;
	private boolean		mDisableEvents;
	private JStructureView[]	mReactantView;

	public UIDelegateCLib(DEFrame parent) {
		mParent = parent;
		mTableModel = parent.getTableModel();
		mReactantPaneList = new ArrayList<>();
		}

	@Override
	public JComponent createDialogContent() {
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addChangeListener(this);

		int gap = HiDPIHelper.scale(8);
		double[][] size1 = { {gap, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, TableLayout.FILL, TableLayout.PREFERRED, gap, TableLayout.PREFERRED, gap},
							 {gap, TableLayout.PREFERRED, 2*gap, TableLayout.PREFERRED, gap} };
		JPanel editorPanel = new JPanel();
		editorPanel.setLayout(new TableLayout(size1));

		StereoMolecule mol = new StereoMolecule();
		mol.setFragment(true);
		mDrawPanel = new SwingEditorPanel(mol, GenericEditorArea.MODE_REACTION);
		mDrawPanel.getDrawArea().setClipboardHandler(new ClipboardHandler());
		mDrawPanel.setPreferredSize(new Dimension(HiDPIHelper.scale(800), HiDPIHelper.scale(EDITOR_HEIGHT)));
		mDrawPanel.getDrawArea().addDrawAreaListener(this);
		editorPanel.add(mDrawPanel, "1,1,7,1");

		mComboBoxReaction = new JComboBox();
		mComboBoxReaction.addItem(ITEM_CUSTOM_REACTION);
		for (int i=0; i<REACTION.length; i++)
			mComboBoxReaction.addItem(REACTION[i][0]);
		mComboBoxReaction.setSelectedIndex(0);
		mComboBoxReaction.addItemListener(this);
		editorPanel.add(new JLabel("Use template:"), "1,3");
		editorPanel.add(mComboBoxReaction, "3,3");

		JButton bopen = new JButton(COMMAND_OPEN_REACTION);
		bopen.addActionListener(this);
		editorPanel.add(bopen, "5,3");
		JButton bsave = new JButton(COMMAND_SAVE_REACTION);
		bsave.addActionListener(this);
		editorPanel.add(bsave, "7,3");

		double[][] size2 = { {gap, TableLayout.FILL, TableLayout.PREFERRED, TableLayout.FILL, gap},
							 {gap, TableLayout.PREFERRED, gap, TableLayout.FILL, gap} };
		mReactantPanel = new JPanel();
		mReactantPanel.setLayout(new TableLayout(size2));

		mComboBoxMode = new JComboBox(MODE_TEXT);
		JPanel cbp = new JPanel();
		cbp.add(new JLabel("Generate"));
		cbp.add(mComboBoxMode);
		cbp.add(new JLabel("multiple possible products"));
		mReactantPanel.add(cbp, "2,3");

		tabbedPane.add("Generic Reaction", editorPanel);
		tabbedPane.add("Reactants", mReactantPanel);

		return tabbedPane;
		}

	private void updateReactantPanel() {
		Reaction reaction = mDrawPanel.getDrawArea().getReaction();
		int reactantCount = (reaction == null) ? 0 : reaction.getReactants();

		if (mReactantPaneList.size() == reactantCount) {
			for (int i=0; i<reactantCount; i++) {
				mReactantView[i].structureChanged(reaction.getReactant(i));
				mReactantPaneList.get(i).setCompoundFilter(new SubstructureFilter(reaction.getReactant(i)));
				}
			return;
			}

		if (mReactantPanel.getComponentCount() > 1)
			mReactantPanel.remove(1);

		if (mReactantPaneList.size() > reactantCount)
			for (int i=mReactantPaneList.size()-1; i>=reactantCount; i--)
				mReactantPaneList.remove(i);

		JPanel reactantPanel = new JPanel();
		int gap = HiDPIHelper.scale(8);
		double[] sizeH = {gap, TableLayout.PREFERRED, gap, TableLayout.FILL};
		double[] sizeV = new double[6*reactantCount-1];
		for (int i=0; i<reactantCount; i++) {
			sizeV[6*i] = gap/2;
			sizeV[6*i+1] = HiDPIHelper.scale(74);
			sizeV[6*i+2] = gap/2;
			sizeV[6*i+3] = TableLayout.PREFERRED;
			sizeV[6*i+4] = gap/2;
			if (i != reactantCount-1)
				sizeV[6*i+5] = gap;
			}
		double[][] size = {sizeH, sizeV};
		reactantPanel.setLayout(new TableLayout(size));

		mReactantView = new JStructureView[reactantCount];
		mReactantPaneList.clear();
		for (int i=0; i<reactantCount; i++) {
			mReactantView[i] = new JStructureView(reaction.getReactant(i), DnDConstants.ACTION_COPY_OR_MOVE, DnDConstants.ACTION_NONE);
			mReactantView[i].setClipboardHandler(new ClipboardHandler());
			reactantPanel.add(mReactantView[i], "1,"+(6*i+1));
			JButton bload = new JButton("Suggest...");
			bload.setActionCommand(COMMAND_RETRIEVE+i);
			bload.addActionListener(this);
			reactantPanel.add(bload, "1,"+(6*i+3));

			MoleculeFilter filter = new SubstructureFilter(reaction.getReactant(i));
			CompoundCollectionPane reactantPane = new CompoundCollectionPane<>(new DefaultCompoundCollectionModel.IDCodeWithName(), false);
			reactantPane.setClipboardHandler(new ClipboardHandler());
			reactantPane.setCompoundFilter(filter);
			reactantPane.setEditable(true);

			JMenu columnMenu = new JMenu("Add From Column");
			for (int column=0; column<mTableModel.getTotalColumnCount(); column++) {
				if (mTableModel.isColumnTypeStructure(column)) {
					JMenuItem item = new JMenuItem(mTableModel.getColumnTitle(column));
					final int col = column;
					item.addActionListener(e -> {
						int errorCount = 0;
						ArrayList<StereoMolecule> reactantList = new ArrayList<>();
						for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
							StereoMolecule reactant = mTableModel.getChemicalStructure(mTableModel.getTotalRecord(row), col, CompoundTableModel.ATOM_COLOR_MODE_NONE, null);
							if (filter.moleculeQualifies(reactant))
								reactantList.add(reactant);
							else
								errorCount++;
							}
						if (reactantList.size() != 0)
							reactantPane.getModel().addMoleculeList(reactantList);
						if (errorCount != 0)
							JOptionPane.showMessageDialog(mParent, Integer.toString(errorCount).concat(" compounds were not added, because they don't have the required substructure."));
						});
					columnMenu.add(item);
					}
				}
			reactantPane.addCustomPopupItem(columnMenu);
			mReactantPaneList.add(reactantPane);
			reactantPanel.add(reactantPane, "3,"+(6*i)+",2,"+(6*i+4));
			}

		if (reactantCount <= 3)
			mReactantPanel.add(reactantPanel, "1,1,3,1");
		else {
			JScrollPane scrollPane = new JScrollPane(reactantPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
																	JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			scrollPane.setPreferredSize(new Dimension(scrollPane.getPreferredSize().width, HiDPIHelper.scale(EDITOR_HEIGHT)));
			mReactantPanel.add(scrollPane, "1,1,3,1");
			}
		mReactantPanel.validate();
		}

	@Override
	public void eventHappened(EditorEvent e) {
		if (e.isUserChange() && e.getWhat() == EditorEvent.WHAT_MOLECULE_CHANGED) {
			mDisableEvents = true;
			mComboBoxReaction.setSelectedIndex(0);
			mDisableEvents = false;
			}
		}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (((JTabbedPane)e.getSource()).getSelectedIndex() == 1) {
			updateReactantPanel();
			}
		}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(mComboBoxReaction)) {
			if (!mDisableEvents) {
				String item = (String)e.getItem();
				if (e.getStateChange() == ItemEvent.DESELECTED) {
					if (item.equals(ITEM_CUSTOM_REACTION))
						mCustomReaction = mDrawPanel.getDrawArea().getReaction();
					}
				else {
					if (item.equals(ITEM_CUSTOM_REACTION)) {
						mDrawPanel.getDrawArea().setReaction(mCustomReaction);
						}
					else {
						for (int i=0; i<REACTION.length; i++) {
							if (REACTION[i][0].equals(item)) {
								mDrawPanel.getDrawArea().setReaction(ReactionEncoder.decode(REACTION[i][1], true));
								return;
								}
							}
						}
					}
				}
			}
		}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd.equals(COMMAND_OPEN_REACTION)) {
			File rxnFile = FileHelper.getFile(mParent, "Please select a reaction file",
					FileHelper.cFileTypeRXN | CompoundFileHelper.cFileTypeRD);
			if (rxnFile == null)
				return;

			try {
				Reaction reaction = null;

				if (FileHelper.getFileType(rxnFile.getName()) == FileHelper.cFileTypeRXN) {
					reaction = new RXNFileParser().getReaction(rxnFile);
					}
				else {
					RDFileParser rdfParser = new RDFileParser(rxnFile);
					if (rdfParser.isMoleculeNext()) {
						JOptionPane.showMessageDialog(mParent, "The RD-file seems to contains molecules rather than reactions.");
						return;
						}
					if (rdfParser.isReactionNext()) {
						reaction = rdfParser.getNextReaction();
						}
					}

				if (reaction == null) {
					JOptionPane.showMessageDialog(mParent, "Couldn't read a reaction from the given file!");
					return;
					}

				// allow for query features
				for (int i = 0; i<reaction.getMolecules(); i++)
					reaction.getMolecule(i).setFragment(true);

				mDrawPanel.getDrawArea().setReaction(reaction);
				}
			catch (Exception ex) {}
			return;
			}
		if (cmd.equals(COMMAND_SAVE_REACTION)) {
			Reaction rxn = mDrawPanel.getDrawArea().getReaction();
			if (isReactionValid(rxn))
				new FileHelper(mParent).saveRXNFile(rxn);
			return;
			}
		if (cmd.startsWith(COMMAND_RETRIEVE)) {
			Component parent = mParent;
			while (!(parent instanceof Frame) && parent.getParent() != null)
				parent = parent.getParent();

			int reactant = cmd.charAt(COMMAND_RETRIEVE.length()) - '0';
			String idcode = new Canonizer(mReactantView[reactant].getMolecule()).getIDCode();

			new BuildingBlockRetrievalDialog((Frame)parent, idcode, mReactantPaneList.get(reactant).getModel());
			}
/*		if (cmd.startsWith(COMMAND_OPEN_FILE)) {
			int reactant = cmd.charAt(COMMAND_OPEN_FILE.length()) - '0';

			ArrayList<String[]> idcodeWithNameList = new FileHelper(mParent).readIDCodesWithNamesFromFile(null, true);

			if (idcodeWithNameList != null) {
				Reaction rxn = mDrawPanel.getDrawArea().getReaction();
				SSSearcher searcher = new SSSearcher();
				searcher.setFragment(rxn.getReactant(reactant));
				int matchErrors = 0;
				for (int i=idcodeWithNameList.size()-1; i>=0; i--) {
					searcher.setMolecule(new IDCodeParser().getCompactMolecule(idcodeWithNameList.get(i)[0]));
					if (!searcher.isFragmentInMolecule()) {
						idcodeWithNameList.remove(i);
						matchErrors++;
						}
					}

				if (matchErrors != 0) {
					String message = (idcodeWithNameList.size() == 0) ?
							"None of your file's compounds have generic reactant "+(char)('A'+reactant)+" as substructure.\n"
									+ "Therefore no compound could be added to the reactant list."
							: ""+matchErrors+" of your file's compounds don't contain generic reactant "+(char)('A'+reactant)+" as substructure.\n"
							+ "Therefore these compounds were not added to the reactant list.";
					JOptionPane.showMessageDialog(mParent, message);
					}

				if (idcodeWithNameList.size() != 0) {
					if (mReactantPaneList.get(reactant).getModel().getSize() != 0 && 0 == JOptionPane.showOptionDialog(mParent,
							"Do you want to add these compounds or to replace the current list?",
							"Add Or Replace Compounds", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
							null, new String[] {"Add", "Replace"}, "Replace" ))
						mReactantPaneList.get(reactant).getModel().addCompoundList(idcodeWithNameList);
					else
						mReactantPaneList.get(reactant).getModel().setCompoundList(idcodeWithNameList);
					}
				}
			}*/
		}

	@Override
	public Properties getDialogConfiguration() {
		Properties configuration = new Properties();

		String reaction = ReactionEncoder.encode(mDrawPanel.getDrawArea().getReaction(), true,
												 ReactionEncoder.RETAIN_REACTANT_AND_PRODUCT_ORDER
											   | ReactionEncoder.INCLUDE_COORDS
											   | ReactionEncoder.INCLUDE_MAPPING
											 /*  | ReactionEncoder.INCLUDE_DRAWING_OBJECTS*/);
		if (reaction != null)
			configuration.setProperty(PROPERTY_REACTION, reaction);

		int index = 0;
		for (CompoundCollectionPane<String[]> ccp:mReactantPaneList) {
			StringBuilder sb1 = new StringBuilder();
			StringBuilder sb2 = new StringBuilder();
			CompoundCollectionModel<String[]> model = ccp.getModel();
			for (int i = 0; i < model.getSize(); i++) {
				if (i != 0) {
					sb1.append('\t');
					sb2.append('\t');
					}
				sb1.append(model.getCompound(i)[0]);
				if (model.getCompound(i)[1] != null)
					sb2.append(model.getCompound(i)[1]);
			}
			configuration.setProperty(PROPERTY_REACTANT+(index), sb1.toString());
			configuration.setProperty(PROPERTY_REACTANT_NAME+(index), sb2.toString());
			index++;
		}

		configuration.setProperty(PROPERTY_MODE, MODE_CODE[mComboBoxMode.getSelectedIndex()]);

		return configuration;
	}

	@Override
	public void setDialogConfiguration(Properties configuration) {
		Reaction reaction = ReactionEncoder.decode(configuration.getProperty(PROPERTY_REACTION), false);
		if (reaction != null)
			mDrawPanel.getDrawArea().setReaction(reaction);

		updateReactantPanel();
		int reactantCount = (reaction == null) ? 0 : reaction.getReactants();

		for (int i=0; i<reactantCount; i++) {
			CompoundCollectionPane<String[]> ccp = mReactantPaneList.get(i);
			String[] idcode = configuration.getProperty(PROPERTY_REACTANT+i, "").split("\\t");
			String[] name = configuration.getProperty(PROPERTY_REACTANT_NAME+i, "").split("\\t");
			for (int j=0; j<idcode.length; j++) {
				String[] idcodeWithName = new String[2];
				idcodeWithName[0] = idcode[j];
				idcodeWithName[1] = (j >= name.length || name[j].length() == 0) ? Integer.toString(j+1) : name[j];
				ccp.getModel().addCompound(idcodeWithName);
				}
			}

		mComboBoxMode.setSelectedIndex(AbstractTask.findListIndex(configuration.getProperty(PROPERTY_MODE), MODE_CODE, 0));
		}

	@Override
	public void setDialogConfigurationToDefault() {
		mDrawPanel.getDrawArea().clearAll();
		updateReactantPanel();
		mComboBoxMode.setSelectedIndex(0);
		}

	private boolean isReactionValid(Reaction rxn) {
		try {
			if (rxn.getReactants() < 1)
				throw new Exception("For combinatorial enumeration you need at least one reactant.");
			if (rxn.getReactants() > 4)
				throw new Exception("Combinatorial enumeration is limited to a maximum of 4 reactants.");
			if (rxn.getProducts() == 0)
				throw new Exception("No product defined.");
			rxn.validateMapping();
			}
		catch (Exception e) {
			JOptionPane.showMessageDialog(mParent, e);
			return false;
			}
		return true;
		}
	}
