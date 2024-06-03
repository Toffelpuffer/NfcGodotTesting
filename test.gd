extends Node

var _bear_sprite = preload("res://sprites/bear.png")
var _crow_sprite = preload("res://sprites/crow.png")

var _plugin_name = "InjectedNfcTestPlugin"
var _plugin_singleton

func _init():
	pass

# Called when the node enters the scene tree for the first time.
func _ready():
	$OutputSprite.texture = null
	
	if Engine.has_singleton(_plugin_name):
		_plugin_singleton = Engine.get_singleton(_plugin_name)
		_plugin_singleton.tagRead.connect(_on_nfc_tag_read)
		$OutputLabel.text = "[Awaiting NFC Tag Connection]"
	else:
		printerr("Initialization error: unable to access the java logic")
		$OutputLabel.text = "[Unable to access android logic]"
	

# Called every frame. 'delta' is the elapsed time since the previous frame.
func _process(delta):
	pass

func _on_nfc_tag_read(text):
	$OutputLabel.text = text
	if text == "bear":
		$OutputSprite.texture = _bear_sprite
	elif text == "crow":
		$OutputSprite.texture = _crow_sprite
	else:
		$OutputSprite.texture = null
