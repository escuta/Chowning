/*
* Mosca: SuperCollider class by Iain Mott, 2016. Licensed under a
* Creative Commons Attribution-NonCommercial 4.0 International License
* http://creativecommons.org/licenses/by-nc/4.0/
* The class makes extensive use of the Ambisonic Toolkit (http://www.ambisonictoolkit.net/)
* by Joseph Anderson and the Automation quark
* (https://github.com/neeels/Automation) by Neels Hofmeyr.
* Required Quarks : Automation, Ctk, XML and  MathLib
* Required classes:
* SC Plugins: https://github.com/supercollider/sc3-plugins
* User must set up a project directory with subdirectoties "ir" and "auto"
* irs should have the first 100 or 120ms silenced to act as "tail" reverberators
* and must be placed in the "ir" directory.
* Run help on the "Mosca" class in SuperCollider for detailed information
* and code examples. Further information and sample irs and B-format recordings
* may be downloaded here: http://escuta.org/mosca
*/

MoscaEffects {
	var <defs, <effectList;
	var <gBfBus, <gBxBus, <transformGrp, globalFx;
	var encodeFunc, decodeFunc;
	var <ossiaGlobal, <ossiaDelay, <ossiaDecay;

	*new { ^super.new().ctr(); }

	ctr { | irBank |

		defs = Array.newFrom(EffectDef.subclasses);

		if (irBank.isNil) {

			defs.removeAt(defs.detectIndex({ | item |
				item == ConvolutionDef.asClass; }));

			effectList = defs.collect({ | item | item.key; });
		} {
			effectList = defs;

			effectList.removeAt(effectList.detectIndex({ | item |
				item == ConvolutionDef.asClass; }));
		};

		defs.removeAt(defs.detectIndex({ | item |
			item == ClearDef.asClass; }));
	}

	setup { | server, sourceGroup, multyThread, maxOrder, renderer, irBank |
		var busChans = MoscaUtils.fourOrNine(maxOrder);

		if (irBank.notNil) { this.prLoadir(server, maxOrder, irBank); };

		gBfBus = Bus.audio(server, busChans); // global b-format bus
		gBxBus = Bus.audio(server, busChans); // global n3d b-format bus
		transformGrp = ParGroup.after(sourceGroup);

		if (multyThread) {

		} {
			if (maxOrder == 1) {

				defs = defs.collect({ | item | item.new1stOrder() });

				decodeFunc = {
					var sigf = In.ar(gBfBus, 4);
					var sigx = In.ar(gBxBus, 4);
					sigx = FoaEncode.ar(sigx, MoscaUtils.n2f());
					sigf = sigf + sigx;
					FoaDecode.ar(sigf, MoscaUtils.b2a());
				};

				if (renderer.format != \FUMA) {
					var enc = MoscaUtils.foa_n3d_encoder();

					encodeFunc = { | sig, gate |
						var convsig = sig * enc;
						convsig = convsig * EnvGen.kr(Env.asr(1), gate, doneAction:2);
						Out.ar(renderer.n3dBus, convsig);
					};
				} {
					encodeFunc = { | sig, gate |
						var convsig = FoaEncode.ar(sig, MoscaUtils.a2b());
						convsig = convsig * EnvGen.kr(Env.asr(1), gate, doneAction:2);
						Out.ar(renderer.fumaBus, convsig);
					};
				};
			} {
				defs = defs.collect({ | item | item.new1stOrder(); });

				decodeFunc = {
					var sigf = In.ar(gBfBus, 9);
					var sigx = In.ar(gBxBus, 9);
					sigx = HOAConvert.ar(2, sigx, \FuMa, \ACN_N3D);
					sigf = sigf + sigx;
					AtkMatrixMix.ar(sigf, MoscaUtils.soa_a12_decoder_matrix());
				};

				if (renderer.format != \FUMA) {
					var enc = MoscaUtils.soa_n3d_encoder();

					encodeFunc = { | sig, gate |
						var convsig = sig * enc;
						convsig = convsig * EnvGen.kr(Env.asr(1), gate, doneAction:2);
						Out.ar(renderer.n3dBus, convsig);
					};
				} {
					encodeFunc = { | sig, gate |
						var convsig = AtkMatrixMix.ar(sig,
							MoscaUtils.soa_a12_encoder_matrix());
						convsig = convsig * EnvGen.kr(Env.asr(1), gate, doneAction:2);
						Out.ar(renderer.fumaBus, convsig);
					};
				};
			};
		};
	}

	prLoadIr { | server, maxOrder, irBank | // prepare list of impulse responses for local and global reverb
		var def;

		if (maxOrder == 1) { def = IrDef; } { def = Ir12chanDef; };

		PathName(irBank).entries.do({ | ir | effectList.add(def(server, ir)); });
	}

	sendFx { | multyThread, server |

		if (multyThread) {
		} {
			defs.do({ | item |

				SynthDef(\globalFx ++ item.key, { | gate = 1, room = 0.5, damp = 0.5,
					a0ir, a1ir, a2ir, a3ir, a4ir, a5ir, a6ir, a7ir, a8ir, a9ir, a10ir, a11ir |
					var sig = decodeFunc.value();
					sig = item.globalFunc.value(sig, room, damp, a0ir, a1ir, a2ir, a3ir, a4ir,
						a5ir, a6ir, a7ir, a8ir, a9ir, a10ir, a11ir);
					encodeFunc.value(sig, gate);
				}).send(server);
			});
		};
	}

	setParam { | ossiaParent, allCritical |

		ossiaGlobal = OssiaAutomationProxy(ossiaParent, "Global_effect", String,
			[nil, nil,
				effectList.collect({ | item |
					if (item.class == String) { item; } { item.key; };
				};
		)], "Clear", critical:true, repetition_filter:true);

		ossiaGlobal.node.description_(effectList.asString);

		ossiaDelay = OssiaAutomationProxy(ossiaGlobal.node, "Delay", Float,
			[0, 1], 0.5, 'clip', critical:allCritical, repetition_filter:true);

		ossiaDecay = OssiaAutomationProxy(ossiaGlobal.node, "Decay", Float,
			[0, 1], 0.5, 'clip', critical:allCritical, repetition_filter:true);
	}

	setAction {

		ossiaGlobal.action_({ | num |

			if (globalFx.isPlaying) { globalFx.set(\gate, 0) };

			if (num.value != "Clear") {
				var synthArgs, i = ossiaGlobal.node.domain.values().detectIndex({ | item | item == num.value });

				if (effectList[i].class != String) { synthArgs = effectList[i].irSpecPar(); };

				globalFx = Synth(\globalFx ++ num.value,
					[\gate, 1, \room, ossiaDelay.value, \damp, ossiaDecay.value] ++
					synthArgs, transformGrp).register;
			};
		});

		ossiaDelay.action_({ | num | if (globalFx.isPlaying) { globalFx.set(\room, num.value); }; });

		ossiaDecay.action_({ | num | if (globalFx.isPlaying) { globalFx.set(\damp, num.value); }; });
	}

	dockTo { | automation |

		automation.dock(ossiaGlobal, "globProxy");
		automation.dock(ossiaDelay, "globDelProxy");
		automation.dock(ossiaDecay, "globDecProxy");
	}
}