<?php

namespace App\Models;

class Tempinfo extends \CodeIgniter\Model
{
    protected $table = 'Tempinfo';
    protected $allowedFields = ['findpw_number', 'email', 'name', 'vnum_expire_time' ];
    protected $primaryKey = 'email';

}