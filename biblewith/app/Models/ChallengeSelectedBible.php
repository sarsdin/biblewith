<?php

namespace App\Models;

class ChallengeSelectedBible extends \CodeIgniter\Model
{
    protected $table = 'ChallengeSelectedBible';
    protected $allowedFields = [
        'chal_no',
        'book'
    ];
    protected $primaryKey = 'chal_no';
}