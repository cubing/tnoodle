<?
require_once "lib_ref_admin.php";
session_start();
require_once "inc_private.php";
$test = preg_match("~^test\\.~i",$_SERVER["HTTP_HOST"]);
$fname = DIR_UPLOADS_ABS.($test?"test_":"")."bg_" . $_SESSION["c_id"] . ".jpg";
if (file_exists($fname)) unlink($fname);
?>