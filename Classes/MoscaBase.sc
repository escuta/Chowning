MoscaBase // acts as the public interface
{
	var dur, <server, <ossiaParent, gui, tracker; // initial rguments
	var renderer, <effects, center, <sources, srcGrp;
	var convertor, virtualAmbi, needConvert = 0, needVirtualAmbi = 0;
	var ossiaMasterPlay, ossiaMasterLib, ossiaTrack, dependant;
	var <control, sysex, <slaved = false, ossiaAutomation, ossiaPlay, // automation control
	ossiaLoop, ossiaTransport, ossiaSync, ossiaRec, ossiaSeekBack, watcher;

	*printSynthParams
	{
		("Parameters usable in SynthDefs
			\\amp | amplitude [0, 4]
			\\dopamnt | Doppler ammount [0, 1]
			\\angle | Stereo angle | default 1.05 (60 degrees) [0, pi]
			\\glev | Global effect level [0, 1]
			\\llev | Local effect level [0, 1]
			\\room | Local reverb room/delay [0, 1]
			\\damp | Local reverb damp/decay [0, 1]
			\\radAzimElev | radius, azimuth and elevation array in radiants [[0,20],[-pi,pi],[-hafPi,halfPi]]
			\\rotAngle | B-format rotation angle [-pi, pi]
			\\directang | B-format directivity [0, pi/2]
			\\contr | Contraction of the soundfiled, from omnidirectional to punctual [0, 1]
			\\aux | aray of 5 Auxiliary parameter values [[0, 1],[0, 1],[0, 1],[0, 1],[0, 1]]
			\\check | array of 5 Auxiliary Boolean values [[false,true],[false,true],[false,true],[false,true],[false,true]]
		").postln;

	}

	addSource
	{ | addN = 1 |

		addN.do({
			var newSource, spatList, previousSource = sources.get.last;

			spatList = previousSource.library.node.domain.values; // get spatlist

			newSource = MoscaSource(previousSource.index + 1, server, srcGrp, ossiaParent,
				previousSource.localAmount.node.critical, // get "allCritical" argument
				spatList, effects.ossiaGlobal.node.domain.values());

			newSource.setAction(effects.effectList, spatList, center, ossiaPlay);

			newSource.addDependant(dependant);

			newSource.dockTo(control);

			sources.set(sources.get.add(newSource));

			if (gui.notNil) { { gui.addSource(sources.get.last) }.defer };
		})
	}

	removeSource
	{ | removeN = 1 |

		if (sources.get.size > 1)
		{
			removeN.do({
				var i, src = sources.get.removeAt(sources.get.size - 1);

				i = src.index;

				src.free;

				if (gui.notNil) { { gui.removeSource(i) }.defer };
			})
		}
	}

	playAutomation
	{ | loop = false |

		ossiaLoop.v_(loop);
		control.play();
	}

	stopAutomation
	{
		control.stop();
	}

	saveAutomation
	{ | directory |

		if (directory.notNil && (directory != control.presetDir))
		{ control.presetDir = PathName(directory).parentPath };

		control.save(control.presetDir);
	}

	loadAutomation
	{ | directory |

		if (directory.notNil && (directory != control.presetDir))
		{ control.presetDir = PathName(directory).parentPath };

		control.load(control.presetDir);

		// seek at curent time to apply the values loaded
		control.seek(control.now);
	}

	slaveToMMC
	{ | enable = false |

		if (enable)
		{
			if (slaved.not)
			{
				"Slaving transport to MMC".postln;
				MIDIIn.addFuncTo(\sysex, sysex);
			};

			slaved = true;
		} {
			if (slaved)
			{
				"MIDI input closed".postln;
				MIDIIn.removeFuncFrom(\sysex, sysex);
			};

			slaved = false;
		};
	}

	syncFiles { | boolean = false | ossiaSync(boolean) }

	recordAudio
	{ | blips = false, channels = 2, bus |

		if (blips) { this.prBlips };

		if (bus.isNil) { bus = renderer.recBus };

		server.record((PathName(control.presetDir).parentPath
			++ "MoscaOut.wav").standardizePath, bus, channels);
	}

	stopRecording
	{
		server.stopRecording;
		"Recording stopped".postln;
	}

	inputFile
	{ | sourceNum = 1, path = "", stream = false |

		var src = this.prGetSource(sourceNum);

		src.file.valueAction_(path);
		src.stream.valueAction_(stream);
	}

	inputExternal
	{ | sourceNum = 1, numChanels = 1, boolean = false |

		var src = this.prGetSource(sourceNum);

		src.nChan.valueAction_(numChanels);
		src.external.valueAction_(boolean);
	}

	inputScSynths
	{ | sourceNum = 1, numChanels = 1, boolean = false |

		var src = this.prGetSource(sourceNum);

		src.nChan.valueAction_(numChanels);
		src.scSynths.valueAction_(boolean);
	}

	setCartesian
	{ | sourceNum = 1, x = 0, y = 20, z = 0 |

		var src = this.prGetSource(sourceNum);

		src.coordinates.cartesian.value_([x, y, z]);
	}

	setAzElDist
	{ | sourceNum = 1, azimuth = 0, elevation = 0, distance = 20 |

		var src = this.prGetSource(sourceNum);

		src.coordinates.azElDist.value_([azimuth, elevation, distance]);
	}

	setLibrary
	{ | sourceNum = 1, library = "Ambitools" |

		var src = this.prGetSource(sourceNum);

		src.library.valueAction_(library);
	}

	play
	{ | sourceNum = 1, boolean = false |

		var src = this.prGetSource(sourceNum);

		src.play.value_(boolean);
	}

	setLoop
	{ | sourceNum = 1, boolean = false |

		var src = this.prGetSource(sourceNum);

		src.loop.valueAction_(boolean);
	}

	setLevel
	{ | sourceNum = 1, db = 0 |

		var src = this.prGetSource(sourceNum);

		src.level.valueAction_(db);
	}

	setContraction
	{ | sourceNum = 1, contraction = 1 |

		var src = this.prGetSource(sourceNum);

		src.contraction.valueAction_(contraction);
	}

	setDoppler
	{ | sourceNum = 1, amount = 0 |

		var src = this.prGetSource(sourceNum);

		src.doppler.valueAction_(amount);
	}

	setGlobalAmount
	{ | sourceNum = 1, amount = 0 |

		var src = this.prGetSource(sourceNum);

		src.globalAmount.valueAction_(amount);
	}

	setLocalEffect
	{ | sourceNum = 1, effect = "Clear" |

		var src = this.prGetSource(sourceNum);

		src.localEffect.valueAction_(effect);
	}

	setLocalAmount
	{ | sourceNum = 1, amount = 0 |

		var src = this.prGetSource(sourceNum);

		src.localAmount.valueAction_(amount);
	}

	setLocalDelay
	{ | sourceNum = 1, delay = 0.5 |

		var src = this.prGetSource(sourceNum);

		src.localDelay.valueAction_(delay);
	}

	setLocalDecay
	{ | sourceNum = 1, decay = 0.5 |

		var src = this.prGetSource(sourceNum);

		src.localDecay.valueAction_(decay);
	}

	setStereoAngle
	{ | sourceNum = 1, angle = 60 |

		var src = this.prGetSource(sourceNum);

		src.angle.valueAction_(angle);
	}

	setBFormatRotation
	{ | sourceNum = 1, rotation = 0 |

		var src = this.prGetSource(sourceNum);

		src.rotation.valueAction_(rotation);
	}

	setBFormatDirectivity
	{ | sourceNum = 1, directivity = 0 |

		var src = this.prGetSource(sourceNum);

		src.directivity.valueAction_(directivity);
	}

	setAtkSpread
	{ | sourceNum = 1, boolean = false |

		var src = this.prGetSource(sourceNum);

		src.spread.valueAction_(false);
	}

	setAtkDiffuse
	{ | sourceNum = 1, boolean = false |

		var src = this.prGetSource(sourceNum);

		src.diffuse.valueAction_(false);
	}

	setJoshRate
	{ | sourceNum = 1, rate = 10 |

		var src = this.prGetSource(sourceNum);

		src.rate.valueAction_(rate);
	}

	setJoshWindow
	{ | sourceNum = 1, size = 0.1 |

		var src = this.prGetSource(sourceNum);

		src.window.valueAction_(size);
	}

	setJoshRAndom
	{ | sourceNum = 1, randomize = 0 |

		var src = this.prGetSource(sourceNum);

		src.random.valueAction_(randomize);
	}

	setAuxilary
	{ | sourceNum = 1, auxNum = 1, value = 0 |

		var i, src = this.prGetSource(sourceNum);

		i = prGetAuxIndex(auxNum);

		src.aux[i].valueAction_(value);
	}

	setAuxCheck
	{ | sourceNum = 1, auxNum = 1, boolean = false |

		var i, src = this.prGetSource(sourceNum);

		i = prGetAuxIndex(auxNum);

		src.check[i].valueAction_(boolean);
	}

	prGetSource
	{ | sourceNum |

		if (sourceNum.isInteger)
		{
			^sources.get[(sourceNum.clip(1, sources.get.size)) - 1];
		} {
			^Error("index must be an Integer").throw;
		};
	}

	prGetAuxIndex
	{ | auxNum |

		if (auxNum.isInteger)
		{
			^(auxNum.clip(1, 5) - 1);
		} {
			^Error("index must be an Integer").throw;
		};
	}

	// These methods relate to control of synths when SW Input delected
	// Set by user. Registerred functions called by "play"

	embedSynth
	{ | sourceNum = 1, numChans = 1, triggerFunc, stopFunc, register |

		var src = this.prGetSource(sourceNum);

		src.nChan.valueAction_(numChans);
		src.scSynths.valueAction_(true);
		src.triggerFunc = triggerFunc;
		src.stopFunc = stopFunc;
		src.synthRegistry.clear;
		src.synthRegistry.add(register);
	}

	clearEmbededSynth
	{ | sourceNum = 1 |

		var src = this.prGetSource(sourceNum);

		src.triggerFunc = nil;
		src.stopFunc = nil;
		src.synthRegistry.clear;
	}

	getSCBus
	{ | sourceNum = 1, numChans = 1 |

		var src = this.prGetSource(sourceNum);

		src.nChan.valueAction_(numChans);
		src.scSynths.valueAction_(true);
		^src.scInBus.index;
	}

	gui
	{ | size = 800, palette = \ossia, lag = 0.05 |

		if (gui.isNil)
		{
			gui = MoscaGUI(this, size, palette, lag);

			gui.win.onClose_({
				gui.free;
				gui = nil;
			});
		}
	}

	headTracker
	{ | port = "/dev/ttyUSB0", offsetheading = 0, type = \orient, extraArgs |

		if (tracker.isNil)
		{
			switch (type,
				\gps,
				{ tracker = HeadTrackerGPS(center, ossiaTrack, port, offsetheading) },
				\orient,
				{ tracker = HeadTracker(center, ossiaTrack, port, offsetheading) },
				\pozyxOSC,
				{ tracker = PozyxOSC(center, ossiaTrack, port, extraArgs) }
			);
		}
	}

	freeHeadTracker
	{
		if (tracker.notNil)
		{
			tracker.free();
			tracker = nil;
		};
	}

	track
	{ | enable = true |

		if (ossiaTrack.value != enable) { ossiaTrack.value = enable };
	}
}
